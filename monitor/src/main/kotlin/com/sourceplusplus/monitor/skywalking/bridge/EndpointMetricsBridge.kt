package com.sourceplusplus.monitor.skywalking.bridge

import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.model.GetEndpointMetrics
import com.sourceplusplus.monitor.skywalking.model.GetMultipleEndpointMetrics
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import monitor.skywalking.protocol.metrics.GetLinearIntValuesQuery
import monitor.skywalking.protocol.metrics.GetMultipleLinearIntValuesQuery

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
