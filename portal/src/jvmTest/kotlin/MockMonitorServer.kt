import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.sourceplusplus.portal.extensions.*
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClickedDisplaySpanInfo
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClickedDisplayTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClickedDisplayTraces
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClickedViewAsExternalPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ConfigurationTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.OverviewTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.RealOverviewTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.SetActiveChartMetric
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.TracesTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.ClearOverview
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.DisplayArtifactConfiguration
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.UpdateEndpoints
import com.sourceplusplus.protocol.artifact.trace.*
import com.sourceplusplus.protocol.portal.*
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import java.time.Instant
import java.util.*
import java.util.concurrent.ThreadLocalRandom.current

var currentMetricType = MetricType.ResponseTime_Average

fun main() {
    DatabindCodec.mapper().registerModule(GuavaModule())
    DatabindCodec.mapper().registerModule(Jdk8Module())
    DatabindCodec.mapper().registerModule(JavaTimeModule())
    DatabindCodec.mapper().enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
    DatabindCodec.mapper().enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)

    val vertx = Vertx.vertx()
    val sockJSHandler = SockJSHandler.create(vertx)
    val portalBridgeOptions = SockJSBridgeOptions()
        .addInboundPermitted(PermittedOptions().setAddressRegex(".+"))
        .addOutboundPermitted(PermittedOptions().setAddressRegex(".+"))
    sockJSHandler.bridge(portalBridgeOptions)

    val router = Router.router(vertx)
    router.route("/eventbus/*").handler(sockJSHandler)

    vertx.createHttpServer().requestHandler(router).listen(8888, "localhost")

    vertx.eventBus().consumer<String>(ClickedViewAsExternalPortal) {
        it.reply(JsonObject().put("portalUuid", "null"))
    }

    vertx.eventBus().consumer<Void>(RealOverviewTabOpened) {
        displayEndpoints(vertx)
    }

    vertx.eventBus().consumer<Void>(OverviewTabOpened) {
        updateCards(vertx)

        vertx.eventBus().publish(ClearOverview("null"), "")
        displayChart(vertx)
    }
    vertx.setPeriodic(2500) {
        updateCards(vertx)
        displayChart(vertx)
    }

    vertx.eventBus().consumer<JsonObject>(SetActiveChartMetric) {
        currentMetricType = MetricType.valueOf(it.body().getString("metricType"))
        updateCards(vertx)
        displayChart(vertx)
    }

    vertx.eventBus().consumer<Void>(ClickedDisplayTraces) {
        displayTraces(vertx)
    }
    vertx.eventBus().consumer<Void>(TracesTabOpened) {
        displayTraces(vertx)
    }

    vertx.eventBus().consumer<String>(ClickedDisplayTraceStack) {
        val traceSpans = mutableListOf<TraceSpanInfo>()
        for (i in 1..5) {
            val span = TraceSpan(
                artifactQualifiedName = UUID.randomUUID().toString(),
                parentSpanId = System.currentTimeMillis().toInt(),
                traceId = "100",
                segmentId = "100",
                spanId = 100,
                error = current().nextBoolean(),
                hasChildStack = false,
                startTime = Instant.now().toEpochMilli(), //todo: use Instant instead of long
                component = "DATABASE",
                endTime = System.currentTimeMillis(),
                serviceCode = "SERVICE_CODE",
                type = "TYPE"
            )
            val spanInfo = TraceSpanInfo(
                span = span,
                appUuid = "null",
                rootArtifactQualifiedName = UUID.randomUUID().toString(),
                operationName = UUID.randomUUID().toString(),
                timeTook = "10s",
                totalTracePercent = current().nextDouble(100.0)
            )
            traceSpans.add(spanInfo)
        }
        vertx.eventBus().displayTraceStack("null", traceSpans)
    }

    vertx.eventBus().consumer<Void>(ClickedDisplaySpanInfo) {
        val span = TraceSpan(
            traceId = "100-" + System.currentTimeMillis(),
            parentSpanId = System.currentTimeMillis().toInt(),
            spanId = System.currentTimeMillis().toInt(),
            segmentId = "100",
            startTime = Instant.now().toEpochMilli(), //todo: use Instant instead of long
            endTime = Instant.now().toEpochMilli(), //todo: use Instant instead of long
            serviceCode = "SERVICE_CODE",
            type = "TYPE",
            tags = mapOf(
                "thing1" to UUID.randomUUID().toString(),
                "thing2" to UUID.randomUUID().toString(),
                "thing3" to UUID.randomUUID().toString(),
                "thing4" to UUID.randomUUID().toString(),
                "thing5" to UUID.randomUUID().toString()
            ),
            logs = listOf(
                TraceSpanLogEntry(time = Clock.System.now().toEpochMilliseconds(), data = UUID.randomUUID().toString())
            )
        )
        vertx.eventBus().displaySpanInfo("null", span)
    }

    vertx.eventBus().consumer<Void>(ConfigurationTabOpened) {
        vertx.eventBus().publish(
            DisplayArtifactConfiguration("null"), JsonObject()
                .put("artifact_qualified_name", UUID.randomUUID().toString())
                .put("create_date", Instant.now().epochSecond)
                .put("last_updated", Instant.now().epochSecond)
                .put(
                    "config", JsonObject()
                        .put("endpoint", current().nextBoolean())
                        .put("subscribe_automatically", current().nextBoolean())
                        .put("endpoint_name", UUID.randomUUID().toString())
                )
        )
    }
}

fun displayEndpoints(vertx: Vertx) {
    vertx.eventBus().send(
        UpdateEndpoints("null"), JsonObject(
            "{\"endpointMetrics\":[{\"appUuid\":\"null\",\"artifactQualifiedName\":\"spp.example.webapp.controller.WebappController.throwsException()\",\"timeFrame\":\"LAST_5_MINUTES\",\"start\":1602276441,\"stop\":1602276741,\"step\":\"MINUTE\",\"artifactMetrics\":[{\"metricType\":\"Throughput_Average\",\"values\":[60,60,60,60,60,16]},{\"metricType\":\"ResponseTime_Average\",\"values\":[2,2,2,2,1,1]},{\"metricType\":\"ServiceLevelAgreement_Average\",\"values\":[0,0,0,0,0,0]}]},{\"appUuid\":\"null\",\"artifactQualifiedName\":\"spp.example.webapp.controller.WebappController.createUser(java.lang.String,java.lang.String)\",\"timeFrame\":\"LAST_5_MINUTES\",\"start\":1602276442,\"stop\":1602276742,\"step\":\"MINUTE\",\"artifactMetrics\":[{\"metricType\":\"Throughput_Average\",\"values\":[300,300,300,300,300,82]},{\"metricType\":\"ResponseTime_Average\",\"values\":[0,0,0,0,0,0]},{\"metricType\":\"ServiceLevelAgreement_Average\",\"values\":[10000,10000,10000,10000,10000,10000]}]},{\"appUuid\":\"null\",\"artifactQualifiedName\":\"spp.example.webapp.controller.WebappController.userList()\",\"timeFrame\":\"LAST_5_MINUTES\",\"start\":1602276442,\"stop\":1602276742,\"step\":\"MINUTE\",\"artifactMetrics\":[{\"metricType\":\"Throughput_Average\",\"values\":[3000,3000,3000,3000,3000,817]},{\"metricType\":\"ResponseTime_Average\",\"values\":[5,6,7,7,7,9]},{\"metricType\":\"ServiceLevelAgreement_Average\",\"values\":[10000,10000,10000,10000,10000,10000]}]},{\"appUuid\":\"null\",\"artifactQualifiedName\":\"spp.example.webapp.controller.WebappController.getUser(long)\",\"timeFrame\":\"LAST_5_MINUTES\",\"start\":1602276443,\"stop\":1602276743,\"step\":\"MINUTE\",\"artifactMetrics\":[{\"metricType\":\"Throughput_Average\",\"values\":[6000,6000,6000,6000,6000,1635]},{\"metricType\":\"ResponseTime_Average\",\"values\":[0,0,0,0,0,0]},{\"metricType\":\"ServiceLevelAgreement_Average\",\"values\":[9986,9995,9993,9998,9980,9993]}]}]}"
        )
    )
}

fun displayChart(vertx: Vertx) {
    val seriesData =
        SplineSeriesData(
            0,
            listOf(
                Clock.System.now().toEpochMilliseconds(),
                Clock.System.now().plus(10, DateTimeUnit.SECOND, TimeZone.UTC).toEpochMilliseconds()
            ),
            doubleArrayOf(current().nextDouble(10.0), current().nextDouble(10.0))
        )
    val splineChart = SplineChart(currentMetricType, QueryTimeFrame.LAST_15_MINUTES, listOf(seriesData))
    vertx.eventBus().updateChart("null", splineChart)
}

fun displayTraces(vertx: Vertx) {
    val traces = mutableListOf<Trace>()
    for (i in 1..20) {
        val trace = Trace(
            traceIds = listOf("${current().nextInt()}.${current().nextInt()}"),
            operationNames = listOf(UUID.randomUUID().toString()),
            prettyDuration = "10s",
            duration = 10000,
            error = current().nextBoolean(),
            start = Instant.now().toEpochMilli() //todo: instant instead of long
        )
        traces.add(trace)
    }

    val tracesResult = TraceResult(
        appUuid = "null",
        artifactQualifiedName = UUID.randomUUID().toString(),
        artifactSimpleName = UUID.randomUUID().toString(),
        start = Clock.System.now().toEpochMilliseconds(),
        stop = Clock.System.now().toEpochMilliseconds(),
        total = traces.size,
        traces = traces.toList(),
        orderType = TraceOrderType.LATEST_TRACES
    )
    vertx.eventBus().displayTraces("null", tracesResult)
}

fun updateCards(vertx: Vertx) {
    val throughputAverageCard =
        BarTrendCard(meta = "throughput_average", header = current().nextInt(100).toString())
    val responseTimeAverageCard =
        BarTrendCard(meta = "responsetime_average", header = current().nextInt(100).toString())
    val slaAverageCard =
        BarTrendCard(meta = "servicelevelagreement_average", header = current().nextInt(100).toString())
    vertx.eventBus().displayCard("null", throughputAverageCard)
    vertx.eventBus().displayCard("null", responseTimeAverageCard)
    vertx.eventBus().displayCard("null", slaAverageCard)
}
