/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.monitor.skywalking.bridge

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import monitor.skywalking.protocol.metrics.GetLinearIntValuesQuery
import monitor.skywalking.protocol.metrics.GetMultipleLinearIntValuesQuery
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.model.GetEndpointMetrics
import spp.jetbrains.monitor.skywalking.model.GetMultipleEndpointMetrics

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class EndpointMetricsBridge(private val skywalkingClient: SkywalkingClient) : CoroutineVerticle() {

    override suspend fun start() {
        vertx.eventBus().localConsumer<GetEndpointMetrics>(getMetricsAddress) {
            launch(vertx.dispatcher()) {
                val request = it.body()
                val response: MutableList<GetLinearIntValuesQuery.Result> = ArrayList()
                request.metricIds.forEach {
                    val metric =
                        skywalkingClient.getEndpointMetrics(
                            it,
                            request.endpointId,
                            request.zonedDuration.toDuration(skywalkingClient)
                        )
                    if (metric != null) response.add(metric)
                }
                it.reply(response)
            }
        }

        vertx.eventBus().localConsumer<GetMultipleEndpointMetrics>(getMultipleMetricsAddress) {
            launch(vertx.dispatcher()) {
                val request = it.body()
                it.reply(
                    skywalkingClient.getMultipleEndpointMetrics(
                        request.metricId,
                        request.endpointId,
                        request.numOfLinear,
                        request.zonedDuration.toDuration(skywalkingClient)
                    )
                )
            }
        }
    }

    companion object {
        private const val rootAddress = "monitor.skywalking.endpoint.metrics"
        private const val getMetricsAddress = "$rootAddress.getMetrics"
        private const val getMultipleMetricsAddress = "$rootAddress.getMultipleMetrics"

        suspend fun getMetrics(request: GetEndpointMetrics, vertx: Vertx): List<GetLinearIntValuesQuery.Result> {
            return vertx.eventBus()
                .request<List<GetLinearIntValuesQuery.Result>>(getMetricsAddress, request)
                .await().body()
        }

        suspend fun getMultipleMetrics(
            request: GetMultipleEndpointMetrics,
            vertx: Vertx
        ): List<GetMultipleLinearIntValuesQuery.Result> {
            return vertx.eventBus()
                .request<List<GetMultipleLinearIntValuesQuery.Result>>(getMultipleMetricsAddress, request)
                .await().body()
        }
    }
}
