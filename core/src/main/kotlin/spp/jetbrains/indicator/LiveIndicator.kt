/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
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
package spp.jetbrains.indicator

import com.apollographql.apollo3.exception.ApolloException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import spp.jetbrains.marker.source.mark.api.event.IEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.UserData
import spp.jetbrains.status.SourceStatus.ConnectionError
import spp.jetbrains.status.SourceStatus.Ready
import spp.jetbrains.status.SourceStatusService

@Suppress("unused")
abstract class LiveIndicator(val project: Project) {

    companion object {
        private val log = logger<LiveIndicator>()
    }

    open val listenForAllEvents: Boolean = false
    open val listenForEvents: List<IEventCode> = emptyList()

    private var periodicTimerId = -1L
    val vertx = UserData.vertx(project)
    val skywalkingMonitorService = UserData.skywalkingMonitorService(project)
    val liveViewService = UserData.liveViewService(project)!!

    open suspend fun onRegister() {
        vertx.setPeriodic(5000) { timerId ->
            periodicTimerId = timerId
            GlobalScope.launch(vertx.dispatcher()) {
                if (SourceStatusService.getInstance(project).getCurrentStatus().first != Ready) {
                    log.debug("Not ready, ignoring indicator refresh")
                    return@launch
                }

                try {
                    refreshIndicator()
                } catch (ex: ApolloException) {
                    log.warn("Error refreshing indicator", ex)
                    SourceStatusService.getInstance(project)
                        .update(ConnectionError, "Unable to connect to platform")
                }
            }
        }
    }

    open suspend fun onUnregister() {
        vertx.cancelTimer(periodicTimerId)
    }

    open suspend fun refreshIndicator() = Unit
    open suspend fun trigger(guideMark: GuideMark, event: SourceMarkEvent) = Unit
}
