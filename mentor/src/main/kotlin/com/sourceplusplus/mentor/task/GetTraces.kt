package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorJob.ContextKey
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.model.GetEndpointTraces
import com.sourceplusplus.monitor.skywalking.model.ZonedDuration
import com.sourceplusplus.monitor.skywalking.track.EndpointTracesTracker
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import java.time.ZonedDateTime

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class GetTraces(
    //todo: serviceId/serviceInstanceId
    private val orderType: TraceOrderType,
    private val timeFrame: QueryTimeFrame, //todo: impl start/end in QueryTimeFrame
    private val endpointName: String? = null,
    private val limit: Int = 10
) : MentorTask() {

    companion object {
        val TRACE_RESULT: ContextKey<TraceResult> = ContextKey("GetTraces.TRACE_RESULT")
    }

    override val contextKeys = listOf(TRACE_RESULT)

    override suspend fun executeTask(job: MentorJob) {
        job.log("Executing task: $this")
        job.log(
            "Task configuration\n\t" +
                    "orderType: $orderType\n\t" +
                    "timeFrame: $timeFrame\n\t" +
                    "endpointName: $endpointName\nt\t" +
                    "limit: $limit"
        )

        val traces = EndpointTracesTracker.getTraces(
            GetEndpointTraces(
                endpointName = endpointName,
                appUuid = "null", //todo: likely not necessary
                artifactQualifiedName = "null", //todo: likely not necessary
                orderType = orderType,
                zonedDuration = ZonedDuration( //todo: use timeFrame
                    ZonedDateTime.now().minusMinutes(15),
                    ZonedDateTime.now(),
                    SkywalkingClient.DurationStep.MINUTE
                ),
                pageSize = limit
            ), job.vertx
        )
        job.context.put(TRACE_RESULT, traces)
        job.log("Added context\n\tKey: $TRACE_RESULT\n\tSize: ${traces.traces.size}")
    }
}
