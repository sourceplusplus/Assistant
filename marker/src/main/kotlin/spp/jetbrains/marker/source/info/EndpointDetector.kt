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
package spp.jetbrains.marker.source.info

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import io.vertx.core.Future
import io.vertx.kotlin.coroutines.await
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.jetbrains.monitor.skywalking.SkywalkingMonitorService
import java.util.*

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class EndpointDetector<T : EndpointDetector.EndpointNameDeterminer>(val project: Project) {

    companion object {
        private val log = logger<EndpointDetector<*>>()
        val ENDPOINT_ID = SourceKey<String>("ENDPOINT_ID")
        val ENDPOINT_NAME = SourceKey<String>("ENDPOINT_NAME")
        val ENDPOINT_INTERNAL = SourceKey<Boolean>("ENDPOINT_INTERNAL")
    }

    abstract val detectorSet: Set<T>
    private val skywalkingMonitor = project.getUserData(SkywalkingMonitorService.KEY)!!

    fun getEndpointName(sourceMark: GuideMark): String? {
        return sourceMark.getUserData(ENDPOINT_NAME)
    }

    fun getEndpointId(sourceMark: GuideMark): String? {
        return sourceMark.getUserData(ENDPOINT_ID)
    }

    fun isExternalEndpoint(sourceMark: GuideMark): Boolean {
        return sourceMark.getUserData(ENDPOINT_INTERNAL) == false
    }

    suspend fun getOrFindEndpointId(sourceMark: GuideMark): String? {
        val cachedEndpointId = sourceMark.getUserData(ENDPOINT_ID)
        return if (cachedEndpointId != null) {
            log.trace("Found cached endpoint id: $cachedEndpointId")
            cachedEndpointId
        } else if (sourceMark is MethodGuideMark) {
            getOrFindEndpoint(sourceMark)
            sourceMark.getUserData(ENDPOINT_ID)
        } else {
            null
        }
    }

    suspend fun getOrFindEndpointName(sourceMark: GuideMark): String? {
        val cachedEndpointName = sourceMark.getUserData(ENDPOINT_NAME)
        return if (cachedEndpointName != null) {
            log.trace("Found cached endpoint name: $cachedEndpointName")
            cachedEndpointName
        } else if (sourceMark is MethodGuideMark) {
            getOrFindEndpoint(sourceMark)
            sourceMark.getUserData(ENDPOINT_NAME)
        } else {
            null
        }
    }

    private suspend fun getOrFindEndpoint(sourceMark: MethodGuideMark) {
        if (sourceMark.getUserData(ENDPOINT_NAME) == null || sourceMark.getUserData(ENDPOINT_ID) == null) {
            if (sourceMark.getUserData(ENDPOINT_NAME) == null) {
                log.trace("Determining endpoint name")
                val detectedEndpoint = determineEndpointName(sourceMark)
                if (detectedEndpoint != null) {
                    log.trace("Detected endpoint name: ${detectedEndpoint.name}")
                    sourceMark.putUserData(ENDPOINT_NAME, detectedEndpoint.name)
                    sourceMark.putUserData(ENDPOINT_INTERNAL, detectedEndpoint.internal)

                    determineEndpointId(detectedEndpoint.name, sourceMark)
                } else {
                    log.trace("Could not find endpoint name for: ${sourceMark.artifactQualifiedName}")
                }
            } else {
                determineEndpointId(sourceMark.getUserData(ENDPOINT_NAME)!!, sourceMark)
            }
        }
    }

    private suspend fun determineEndpointId(endpointName: String, sourceMark: MethodGuideMark) {
        log.trace("Determining endpoint id")
        val endpoint = skywalkingMonitor.searchExactEndpoint(endpointName)
        if (endpoint != null) {
            sourceMark.putUserData(ENDPOINT_ID, endpoint.getString("id"))
            log.trace("Detected endpoint id: ${endpoint.getString("id")}")
        } else {
            log.trace("Could not find endpoint id for: $endpointName")
        }
    }

    private suspend fun determineEndpointName(guideMark: MethodGuideMark): DetectedEndpoint? {
        detectorSet.forEach {
            val detectedEndpoint = it.determineEndpointName(guideMark).await()
            if (detectedEndpoint.isPresent) return detectedEndpoint.get()
        }
        return null
    }

    /**
     * todo: description.
     */
    data class DetectedEndpoint(
        val name: String,
        val internal: Boolean,
        val path: String? = null,
        val type: String? = null,
    )

    /**
     * todo: description.
     */
    interface EndpointNameDeterminer {
        fun determineEndpointName(guideMark: MethodGuideMark): Future<Optional<DetectedEndpoint>>
    }
}
