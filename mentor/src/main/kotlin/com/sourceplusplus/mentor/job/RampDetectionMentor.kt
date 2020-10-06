package com.sourceplusplus.mentor.job

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.mentor.task.analyze.CalculateLinearRegression
import com.sourceplusplus.mentor.task.general.DelayTask
import com.sourceplusplus.mentor.task.general.NoopTask
import com.sourceplusplus.mentor.task.monitor.GetEndpoints
import com.sourceplusplus.mentor.task.monitor.GetService
import com.sourceplusplus.mentor.task.monitor.GetServiceInstance
import com.sourceplusplus.mentor.task.monitor.GetTraces
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import io.vertx.core.Vertx

/**
 * Keeps track of endpoint durations for indications of 'The Ramp' [Smith and Williams 2002] performance anti-pattern.
 * Uses a continuous linear regression model which requires a specific confidence before alerting developer.
 * May also indicate root cause by searching trace stack for probable offenders.
 *
 * [Smith and Williams 2002] C. U. Smith and L. G. Williams, Performance Solutions: A Practical Guide to
 * Creating Responsive, Scalable Software, Boston, MA, Addison-Wesley, 2002.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class RampDetectionMentor(
    override val vertx: Vertx,
    confidence: Double = 0.5
) : MentorJob() {

    override val tasks: List<MentorTask> by lazy {
        listOf(
            //get active service instance
            GetService(),
            GetServiceInstance(
                GetService.SERVICE
            ),

            //iterate endpoints (likely offenders more frequently than non-likely offenders)
            GetEndpoints(),
            GetTraces(
                GetEndpoints.ENDPOINT_IDS,
                orderType = TraceOrderType.LATEST_TRACES,
                timeFrame = QueryTimeFrame.LAST_15_MINUTES
            ),

            //keep track of regression of endpoint duration
            CalculateLinearRegression(GetTraces.TRACE_RESULT, confidence), //todo: ARIMA model?

            //todo: search source code of endpoint for culprits

            if (config.repeatForever) {
                DelayTask(config.repeatDelay)
            } else {
                NoopTask()
            }
        )
    }
}