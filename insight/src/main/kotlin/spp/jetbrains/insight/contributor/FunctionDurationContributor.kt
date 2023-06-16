/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.insight.contributor

import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import io.vertx.core.json.JsonObject
import spp.jetbrains.UserData
import spp.jetbrains.artifact.service.ArtifactScopeService
import spp.jetbrains.artifact.service.toArtifact
import spp.jetbrains.insight.InsightKeys.FUNCTION_DURATION
import spp.jetbrains.insight.InsightKeys.FUNCTION_DURATION_PREDICTION
import spp.jetbrains.insight.ProceduralAnalyzer
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarkerKeys.VCS_MODIFIED
import spp.jetbrains.marker.SourceMarkerUtils.doOnReadThread
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.jetbrains.safeExecuteBlocking
import spp.jetbrains.safeLaunch
import spp.jetbrains.status.SourceStatusService
import spp.protocol.artifact.metrics.MetricType.Companion.Endpoint_RespTime_AVG
import spp.protocol.insight.InsightType
import spp.protocol.insight.InsightValue
import spp.protocol.instrument.location.LiveSourceLocation
import spp.protocol.service.SourceServices.Subscribe.toLiveViewSubscription
import spp.protocol.view.LiveView
import spp.protocol.view.LiveViewConfig
import spp.protocol.view.LiveViewEvent

/**
 * Contributes the [FUNCTION_DURATION_PREDICTION] insight. This insight is only calculated for
 * functions that have or call functions with VCS modifications (i.e. [VCS_MODIFIED] flag).
 */
class FunctionDurationContributor(private val remoteInsightsAvailable: Boolean) : SourceMarkEventListener {

    private val log = logger<FunctionDurationContributor>()
    private val updateInsightQueue = MergingUpdateQueue(
        FunctionDurationContributor::javaClass.name, 200, true, null, null
    )

    override fun handleEvent(event: SourceMarkEvent) {
        when (event.eventCode) {
            SourceMarkEventCode.MARK_ADDED -> {
                if (!remoteInsightsAvailable && event.sourceMark is MethodGuideMark) {
                    searchForInsights(event.sourceMark as MethodGuideMark)
                }
            }

            SourceMarkEventCode.MARK_USER_DATA_UPDATED -> {
                if (!remoteInsightsAvailable && EndpointDetector.DETECTED_ENDPOINTS == event.params.firstOrNull()) {
                    UserData.vertx(event.sourceMark.project).safeLaunch {
                        subscribeToResponseTime(event.sourceMark as GuideMark)
                    }
                }
            }

            SourceMarkEventCode.CODE_CHANGED -> {
                when (event.sourceMark) {
                    is MethodGuideMark -> {
                        queueForInsights(event.sourceMark as MethodGuideMark)
                    }
                }
            }
        }
    }

    private fun subscribeToResponseTime(guideMark: GuideMark) {
        val viewService = UserData.liveViewService(guideMark.project) ?: return
        val service = SourceStatusService.getCurrentService(guideMark.project)
        if (service == null) {
            log.warn("No service selected, skipping response time subscription")
            return
        }

        val listenMetrics = listOf(Endpoint_RespTime_AVG.asRealtime().metricId)
        val vertx = UserData.vertx(guideMark.project)
        viewService.addLiveView(
            LiveView(
                null,
                mutableSetOf(guideMark.getUserData(EndpointDetector.DETECTED_ENDPOINTS)!!.firstNotNullOf { it.name }),
                guideMark.artifactQualifiedName,
                LiveSourceLocation(guideMark.artifactQualifiedName.identifier, -1, service.id),
                LiveViewConfig(FUNCTION_DURATION.name, listenMetrics, 1000)
            )
        ).onSuccess {
            val subscriptionId = it.subscriptionId!!
            vertx.eventBus().consumer<JsonObject>(toLiveViewSubscription(subscriptionId)) {
                val viewEvent = LiveViewEvent(it.body())
                val metricsData = JsonObject(viewEvent.metricsData)
                val responseTime = metricsData.getLong("value")
                val currentDuration = guideMark.getUserData(FUNCTION_DURATION)?.value
                if (currentDuration != responseTime) {
                    guideMark.putUserData(
                        FUNCTION_DURATION,
                        InsightValue.of(InsightType.FUNCTION_DURATION, responseTime)
                    )
                    log.trace(
                        "Set method duration from $currentDuration to $responseTime. "
                                + "Artifact: ${guideMark.artifactQualifiedName}"
                    )

                    //propagate to callers
                    vertx.safeExecuteBlocking {
                        ArtifactScopeService.getCallerFunctions(guideMark.getPsiElement())
                            .mapNotNull { it.nameIdentifier?.getUserData(GuideMark.KEY) }
                            .filterIsInstance<MethodGuideMark>().forEach {
                                queueForInsights(it)
                            }
                    }
                }
            }
            guideMark.addEventListener {
                if (it.eventCode == SourceMarkEventCode.MARK_REMOVED) {
                    viewService.removeLiveView(subscriptionId)
                }
            }
        }.onFailure {
            log.warn("Failed to subscribe to response time", it)
        }
    }

    /**
     * todo: rethink
     *
     * This isn't very efficient but is necessary to provide method duration insights for methods that call
     * methods in different files. This code ensures there are existing [GuideMark]s for all user methods
     * that are directly called by the given method.
     */
    private fun searchForInsights(mark: MethodGuideMark) {
        val psiFiles = doOnReadThread {
            val directMethods = ArtifactScopeService.getCalledFunctions(mark.getPsiMethod())
            directMethods.mapNotNull { it.containingFile }.toSet()
        }
        psiFiles.mapNotNull { SourceMarker.getSourceFileMarker(it) } //trigger automatic creation of guide marks
    }

    /**
     * Analyzes the given [MethodSourceMark] to determine [FUNCTION_DURATION_PREDICTION] based on the sum
     * of the average [FUNCTION_DURATION_PREDICTION]/[FUNCTION_DURATION] of the methods it calls.
     *
     * @param mark The method mark to calculate the insight for.
     */
    private fun updateInsights(mark: MethodSourceMark) {
        log.info("Updating method duration prediction insights for: ${mark.artifactQualifiedName}")

        val proceduralPaths = ProceduralAnalyzer().analyze(mark.getPsiMethod().toArtifact()!!)

        //ignore functions with recursive paths
        val hasRecursions = proceduralPaths.any { it.getInsights().any { it.type == InsightType.PATH_IS_RECURSIVE } }
        if (hasRecursions) {
            log.info("Ignoring function with recursive path(s): ${mark.artifactQualifiedName}")
            if (mark.removeUserData(FUNCTION_DURATION_PREDICTION) != null) {
                propagateChange(mark.getPsiMethod())
            }
            return
        }

        val isChildrenChanged = proceduralPaths.flatMap {
            it.getResolvedCallFunctions()
        }.mapNotNull { it.getUserData(VCS_MODIFIED.asPsiKey()) }.any()

        var propagateChange: Boolean
        val isSelfChanged = mark.getUserData(VCS_MODIFIED) ?: false
        if (isSelfChanged || isChildrenChanged) {
            log.info("Artifact ${mark.artifactQualifiedName} is or has callee(s) that is modified")
            propagateChange = true

            //get runtime path insights
            val pathDurationInsights = proceduralPaths.flatMap { it.getInsights() }
                .filter { it.type == InsightType.PATH_DURATION } as List<InsightValue<Long>>

            //average path durations
            val methodDurationPrediction = pathDurationInsights.map { it.value }
                .takeIf { it.isNotEmpty() }?.average()?.toLong()

            //set function duration prediction insight
            if (methodDurationPrediction != null) {
                log.info(
                    "Set method duration prediction to $methodDurationPrediction. " +
                            "Artifact: ${mark.artifactQualifiedName}"
                )
                mark.putUserData(
                    FUNCTION_DURATION_PREDICTION,
                    InsightValue(InsightType.FUNCTION_DURATION_PREDICTION, methodDurationPrediction).asDerived()
                )
            } else {
                propagateChange = mark.removeUserData(FUNCTION_DURATION_PREDICTION) != null
            }
        } else {
            log.debug("Artifact ${mark.artifactQualifiedName} and callee(s) are unmodified")
            propagateChange = mark.removeUserData(FUNCTION_DURATION_PREDICTION) != null
        }

        if (propagateChange) {
            propagateChange(mark.getPsiMethod())
        }
    }

    /**
     * Propagates changes to functions that call the given function.
     */
    private fun propagateChange(function: PsiNameIdentifierOwner) {
        val callerMethods = ArtifactScopeService.getCallerFunctions(function)
        callerMethods.forEach { callerMethod ->
            callerMethod.nameIdentifier?.getUserData(GuideMark.KEY)?.let { callerMark ->
                queueForInsights(callerMark as MethodGuideMark)
            }
        }
    }

    private fun queueForInsights(mark: MethodGuideMark) {
        updateInsightQueue.queue(Update.create(mark) {
            try {
                updateInsights(mark as MethodSourceMark)
            } catch (e: PsiInvalidElementAccessException) {
                //ignore; attempted to get insights for code that's been invalidated
            } catch (e: AssertionError) {
                //ignore; attempted to get insights for code that's been invalidated
            } //todo: should likely be handled by properly using read actions
        })
    }
}
