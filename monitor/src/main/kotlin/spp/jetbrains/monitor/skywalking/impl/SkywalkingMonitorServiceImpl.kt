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
package spp.jetbrains.monitor.skywalking.impl

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.SkywalkingMonitorService
import spp.jetbrains.monitor.skywalking.bridge.EndpointTracesBridge
import spp.jetbrains.monitor.skywalking.bridge.ServiceBridge
import spp.jetbrains.monitor.skywalking.model.TimeInfo
import spp.jetbrains.monitor.skywalking.model.TopNCondition
import spp.jetbrains.monitor.skywalking.model.ZonedDuration
import spp.jetbrains.monitor.skywalking.toProtocol
import spp.protocol.artifact.trace.TraceSpan
import spp.protocol.platform.general.Service

class SkywalkingMonitorServiceImpl(
    private val skywalkingClient: SkywalkingClient
) : SkywalkingMonitorService {

    override suspend fun getTimeInfo(): TimeInfo {
        return skywalkingClient.getTimeInfo().toProtocol()
    }

    override suspend fun searchExactEndpoint(keyword: String, cache: Boolean): JsonObject? {
        val service = getCurrentService() ?: return null
        val endpoints = skywalkingClient.searchEndpoint(keyword, service.id, 10, cache)
        return endpoints.map { it as JsonObject }.find { it.getString("name") == keyword }
    }

    override suspend fun getEndpoints(serviceId: String, limit: Int, cache: Boolean): JsonArray {
        return skywalkingClient.searchEndpoint("", serviceId, limit, cache)
    }

    override suspend fun getTraceStack(traceId: String): List<TraceSpan> {
        return EndpointTracesBridge.getTraceStack(traceId, skywalkingClient.vertx)
    }

    override suspend fun getCurrentService(): Service? {
        return ServiceBridge.getCurrentService(skywalkingClient.vertx)
    }

    override suspend fun sortMetrics(condition: TopNCondition, duration: ZonedDuration, cache: Boolean): JsonArray {
        return skywalkingClient.sortMetrics(condition, duration, cache)
    }
}
