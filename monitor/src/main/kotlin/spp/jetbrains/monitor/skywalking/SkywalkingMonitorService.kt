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
package spp.jetbrains.monitor.skywalking

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import io.vertx.core.AsyncResult
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import monitor.skywalking.protocol.metadata.GetServiceInstancesQuery
import monitor.skywalking.protocol.metadata.GetTimeInfoQuery
import monitor.skywalking.protocol.metrics.GetLinearIntValuesQuery
import monitor.skywalking.protocol.metrics.GetMultipleLinearIntValuesQuery
import monitor.skywalking.protocol.type.TopNCondition
import spp.jetbrains.monitor.skywalking.bridge.LogsBridge
import spp.jetbrains.monitor.skywalking.model.GetEndpointMetrics
import spp.jetbrains.monitor.skywalking.model.GetEndpointTraces
import spp.jetbrains.monitor.skywalking.model.GetMultipleEndpointMetrics
import spp.jetbrains.monitor.skywalking.model.ZonedDuration
import spp.protocol.artifact.log.LogResult
import spp.protocol.artifact.trace.TraceResult
import spp.protocol.artifact.trace.TraceSpanStackQueryResult
import spp.protocol.platform.general.Service

interface SkywalkingMonitorService {
    companion object {
        val KEY = Key.create<SkywalkingMonitorService>("SPP_SKYWALKING_MONITOR_SERVICE")

        fun getInstance(project: Project): SkywalkingMonitorService {
            return project.getUserData(KEY)!!
        }
    }

    suspend fun getVersion(): String
    suspend fun getTimeInfo(): GetTimeInfoQuery.Data
    suspend fun searchExactEndpoint(keyword: String, cache: Boolean = false): JsonObject?
    suspend fun getEndpoints(
        serviceId: String,
        limit: Int,
        cache: Boolean = true
    ): JsonArray

    suspend fun getMetrics(request: GetEndpointMetrics): List<GetLinearIntValuesQuery.Result>
    suspend fun getMultipleMetrics(request: GetMultipleEndpointMetrics): List<GetMultipleLinearIntValuesQuery.Result>
    suspend fun getTraces(request: GetEndpointTraces): TraceResult
    suspend fun getTraceStack(traceId: String): TraceSpanStackQueryResult
    suspend fun queryLogs(query: LogsBridge.GetEndpointLogs): AsyncResult<LogResult>
    suspend fun getCurrentService(): Service?
    suspend fun getActiveServices(): List<Service>
    suspend fun getCurrentServiceInstance(): GetServiceInstancesQuery.Result?
    suspend fun getActiveServiceInstances(): List<GetServiceInstancesQuery.Result>
    suspend fun getServiceInstances(serviceId: String): List<GetServiceInstancesQuery.Result>
    suspend fun sortMetrics(
        condition: TopNCondition,
        duration: ZonedDuration,
        cache: Boolean = true
    ): JsonArray
}
