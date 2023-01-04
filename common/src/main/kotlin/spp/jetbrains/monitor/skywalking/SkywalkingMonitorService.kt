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
package spp.jetbrains.monitor.skywalking

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import spp.jetbrains.monitor.skywalking.model.*
import spp.protocol.artifact.metrics.ArtifactMetrics
import spp.protocol.artifact.trace.TraceResult
import spp.protocol.artifact.trace.TraceSpan
import spp.protocol.platform.general.Service

interface SkywalkingMonitorService {
    companion object {
        val KEY = Key.create<SkywalkingMonitorService>("SPP_SKYWALKING_MONITOR_SERVICE")

        fun getInstance(project: Project): SkywalkingMonitorService {
            return project.getUserData(KEY)!!
        }
    }

    suspend fun getVersion(): String

    suspend fun getTimeInfo(): TimeInfo
    suspend fun searchExactEndpoint(keyword: String, cache: Boolean = false): JsonObject?
    suspend fun getEndpoints(
        serviceId: String,
        limit: Int,
        cache: Boolean = true
    ): JsonArray

    suspend fun getMetrics(request: GetEndpointMetrics): List<ArtifactMetrics>

    //    suspend fun getMultipleMetrics(request: GetMultipleEndpointMetrics): List<GetMultipleLinearIntValuesQuery.Result>
    suspend fun getTraces(request: GetEndpointTraces): TraceResult
    suspend fun getTraceStack(traceId: String): List<TraceSpan>

    //    suspend fun queryLogs(query: LogsBridge.GetEndpointLogs): AsyncResult<LogResult>
    suspend fun getCurrentService(): Service?
    suspend fun getActiveServices(): List<Service>

    //    suspend fun getCurrentServiceInstance(): GetServiceInstancesQuery.Result?
//    suspend fun getActiveServiceInstances(): List<GetServiceInstancesQuery.Result>
    suspend fun getServiceInstances(serviceId: String): List<ServiceInstance>
    suspend fun sortMetrics(
        condition: TopNCondition,
        duration: ZonedDuration,
        cache: Boolean = true
    ): JsonArray
}
