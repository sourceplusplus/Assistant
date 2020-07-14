package com.sourceplusplus.portal.display.tabs

import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.config.SourcePortalConfig
import com.sourceplusplus.api.model.trace.*
import com.sourceplusplus.portal.PortalBootstrap
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.coordinate.track.PortalViewTracker
import com.sourceplusplus.portal.display.PortalTab
import com.sourceplusplus.portal.display.tabs.views.TracesView
import com.sourceplusplus.portal.model.traces.InnerTraceStackInfo
import com.sourceplusplus.portal.model.traces.TraceSpanInfo
import groovy.util.logging.Slf4j
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern

import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.*
import static com.sourceplusplus.api.util.ArtifactNameUtils.*

/**
 * Displays traces (and the underlying spans) for a given source code artifact.
 *
 * @version 0.3.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class TracesTab extends AbstractTab {

    public static final String TRACES_TAB_OPENED = "TracesTabOpened"
    public static final String GET_TRACE_STACK = "GetTraceStack"
    public static final String CLICKED_DISPLAY_TRACES = "ClickedDisplayTraces"
    public static final String CLICKED_DISPLAY_TRACE_STACK = "ClickedDisplayTraceStack"
    public static final String CLICKED_DISPLAY_SPAN_INFO = "ClickedDisplaySpanInfo"
    public static final String DISPLAY_TRACES = "DisplayTraces"
    public static final String DISPLAY_TRACE_STACK = "DisplayTraceStack"
    public static final String DISPLAY_SPAN_INFO = "DisplaySpanInfo"

    private static final Pattern QUALIFIED_NAME_PATTERN = Pattern.compile('.+\\..+\\(.*\\)')

    TracesTab() {
        super(PortalTab.Traces)
    }

    @Override
    void start() throws Exception {
        super.start()

        //refresh with traces from cache (if avail)
        vertx.eventBus().consumer(TRACES_TAB_OPENED, {
            log.info("Traces tab opened")
            def message = JsonObject.mapFrom(it.body())
            def portalUuid = message.getString("portal_uuid")
            def portal = SourcePortal.getPortal(portalUuid)
            if (portal == null) {
                log.warn("Ignoring traces tab opened event. Unable to find portal: {}", portalUuid)
                return
            }

            def orderType = message.getString("trace_order_type")
            if (orderType) {
                //user possibly changed current trace order type; todo: create event
                portal.portalUI.tracesView.orderType = TraceOrderType.valueOf(orderType.toUpperCase())
            }
            portal.portalUI.currentTab = PortalTab.Traces
            SourcePortal.ensurePortalActive(portal)
            updateUI(portal)

            //subscribe (re-subscribe) to get traces as they are created
            def subscribeRequest = ArtifactTraceSubscribeRequest.builder()
                    .appUuid(portal.appUuid)
                    .artifactQualifiedName(portal.portalUI.viewingPortalArtifact)
                    .addOrderTypes(portal.portalUI.tracesView.orderType)
                    .timeFrame(QueryTimeFrame.LAST_5_MINUTES)
                    .build()
            SourcePortalConfig.current.getCoreClient(portal.appUuid).subscribeToArtifact(subscribeRequest, {
                if (it.succeeded()) {
                    log.info("Successfully subscribed to traces with request: {}", subscribeRequest)
                } else {
                    log.error("Failed to subscribe to artifact traces", it.cause())
                }
            })
        })
        vertx.eventBus().consumer(ARTIFACT_TRACE_UPDATED.address, {
            handleArtifactTraceResult(it.body() as ArtifactTraceResult)
        })

        //get historical traces
        vertx.eventBus().consumer(TRACES_TAB_OPENED, {
            def portalUuid = JsonObject.mapFrom(it.body()).getString("portal_uuid")
            def portal = SourcePortal.getPortal(portalUuid)
            if (portal == null) {
                log.warn("Ignoring traces tab opened event. Unable to find portal: {}", portalUuid)
            } else {
                if (portal.external) {
                    portal.portalUI.tracesView.viewTraceAmount = 25
                }

                def traceQuery = TraceQuery.builder()
                        .orderType(portal.portalUI.tracesView.orderType)
                        .pageSize(portal.portalUI.tracesView.viewTraceAmount)
                        .appUuid(portal.appUuid)
                        .artifactQualifiedName(portal.portalUI.viewingPortalArtifact)
                        .durationStart(Instant.now().minus(30, ChronoUnit.DAYS))
                        .durationStop(Instant.now())
                        .durationStep("SECOND").build()
                SourcePortalConfig.current.getCoreClient(portal.appUuid).getTraces(traceQuery, {
                    if (it.succeeded()) {
                        def traceResult = ArtifactTraceResult.builder()
                                .appUuid(traceQuery.appUuid())
                                .artifactQualifiedName(traceQuery.artifactQualifiedName())
                                .orderType(traceQuery.orderType())
                                .start(traceQuery.durationStart())
                                .stop(traceQuery.durationStop())
                                .step(traceQuery.durationStep())
                                .traces(it.result().traces())
                                .total(it.result().total())
                                .build()
                        handleArtifactTraceResult(Collections.singletonList(portal), traceResult)
                    } else {
                        log.error("Failed to get traces", it.cause())
                    }
                })
            }
        })

        //user viewing portal under new artifact
        vertx.eventBus().consumer(PortalViewTracker.CHANGED_PORTAL_ARTIFACT, {
//            def portal = SourcePortal.getPortal(JsonObject.mapFrom(it.body()).getString("portal_uuid"))
//            vertx.eventBus().send(portal.portalUuid + "-ClearTraceStack", new JsonObject())
        })

        //user clicked into trace stack
        vertx.eventBus().consumer(CLICKED_DISPLAY_TRACE_STACK, { messageHandler ->
            def request = messageHandler.body() as JsonObject
            log.debug("Displaying trace stack: {}", request)

            if (request.getString("trace_id") == null) {
                def portal = SourcePortal.getPortal(request.getString("portal_uuid"))
                portal.portalUI.tracesView.viewType = TracesView.ViewType.TRACE_STACK
                updateUI(portal)
            } else {
                vertx.eventBus().request(GET_TRACE_STACK, request, {
                    if (it.failed()) {
                        it.cause().printStackTrace()
                        log.error("Failed to display trace stack", it.cause())
                    } else {
                        def portal = SourcePortal.getPortal(request.getString("portal_uuid"))
                        portal.portalUI.tracesView.viewType = TracesView.ViewType.TRACE_STACK
                        portal.portalUI.tracesView.traceStack = it.result().body() as JsonArray
                        portal.portalUI.tracesView.traceId = request.getString("trace_id")
                        updateUI(portal)
                    }
                })
            }
        })

        vertx.eventBus().consumer(CLICKED_DISPLAY_TRACES, {
            def portal = SourcePortal.getPortal((it.body() as JsonObject).getString("portal_uuid"))
            def representation = portal.portalUI.tracesView
            representation.viewType = TracesView.ViewType.TRACES

            if (representation.innerTraceStack.size() > 0) {
                representation.viewType = TracesView.ViewType.TRACE_STACK
                def stack = representation.innerTraceStack.pop()

                if (representation.innerTrace) {
                    updateUI(portal)
                } else if (!portal.external) {
                    //navigating back to parent stack
                    def rootArtifactQualifiedName = stack.getJsonObject(0).getString("root_artifact_qualified_name")
                    vertx.eventBus().send(NAVIGATE_TO_ARTIFACT.address,
                            new JsonObject().put("portal_uuid", portal.portalUuid)
                                    .put("artifact_qualified_name", rootArtifactQualifiedName)
                                    .put("parent_stack_navigation", true))
                } else {
                    updateUI(portal)
                }
            } else {
                updateUI(portal)
            }
        })

        //user clicked into span
        vertx.eventBus().consumer(CLICKED_DISPLAY_SPAN_INFO, { messageHandler ->
            def spanInfoRequest = messageHandler.body() as JsonObject
            log.debug("Clicked display span info: {}", spanInfoRequest)

            def portalUuid = spanInfoRequest.getString("portal_uuid")
            def portal = SourcePortal.getPortal(portalUuid)
            def representation = portal.portalUI.tracesView
            representation.viewType = TracesView.ViewType.SPAN_INFO
            representation.traceId = spanInfoRequest.getString("trace_id")
            representation.spanId = spanInfoRequest.getInteger("span_id")
            updateUI(portal)
        })

        //query core for trace stack (or get from cache)
        vertx.eventBus().consumer(GET_TRACE_STACK, { messageHandler ->
            def timer = PortalBootstrap.portalMetrics.timer(GET_TRACE_STACK)
            def context = timer.time()
            def request = messageHandler.body() as JsonObject
            def portalUuid = request.getString("portal_uuid")
            def appUuid = request.getString("app_uuid")
            def artifactQualifiedName = request.getString("artifact_qualified_name")
            def globalTraceId = request.getString("trace_id")
            log.trace("Getting trace spans. Artifact qualified name: {} - Trace id: {}",
                    getShortQualifiedFunctionName(artifactQualifiedName), globalTraceId)

            def portal = SourcePortal.getPortal(portalUuid)
            def representation = portal.portalUI.tracesView
            def traceStack = representation.getTraceStack(globalTraceId)
            if (traceStack != null) {
                log.trace("Got trace spans: {} from cache - Stack size: {}", globalTraceId, traceStack.size())
                messageHandler.reply(traceStack)
                context.stop()
            } else {
                def traceStackQuery = TraceSpanStackQuery.builder()
                        .oneLevelDeep(true)
                        .traceId(globalTraceId).build()
                SourcePortalConfig.current.getCoreClient(appUuid).getTraceSpans(appUuid, artifactQualifiedName, traceStackQuery, {
                    if (it.failed()) {
                        log.error("Failed to get trace spans", it.cause())
                    } else {
                        representation.cacheTraceStack(globalTraceId, handleTraceStack(
                                appUuid, artifactQualifiedName, it.result()))
                        messageHandler.reply(representation.getTraceStack(globalTraceId))
                        context.stop()
                    }
                })
            }
        })
        log.info("{} started", getClass().getSimpleName())
    }

    @Override
    void updateUI(SourcePortal portal) {
        if (portal.portalUI.currentTab != thisTab) {
            return
        }

        switch (portal.portalUI.tracesView.viewType) {
            case TracesView.ViewType.TRACES:
                displayTraces(portal)
                break
            case TracesView.ViewType.TRACE_STACK:
                displayTraceStack(portal)
                break
            case TracesView.ViewType.SPAN_INFO:
                displaySpanInfo(portal)
                break
        }
    }

    private void displayTraces(SourcePortal portal) {
        if (portal.portalUI.tracesView.artifactTraceResult) {
            def artifactTraceResult = portal.portalUI.tracesView.artifactTraceResult
            vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_TRACES",
                    new JsonObject(Json.encode(artifactTraceResult)))
            log.debug("Displayed traces for artifact: {} - Type: {} - Trace size: {}",
                    getShortQualifiedFunctionName(artifactTraceResult.artifactQualifiedName()),
                    artifactTraceResult.orderType(), artifactTraceResult.traces().size())
        }
    }

    private void displayTraceStack(SourcePortal portal) {
        def representation = portal.portalUI.tracesView
        def traceId = representation.traceId
        def traceStack = representation.traceStack

       if (representation.innerTrace && representation.viewType != TracesView.ViewType.SPAN_INFO) {
            def innerTraceStackInfo = InnerTraceStackInfo.builder()
                    .innerLevel(representation.innerTraceStack.size())
                    .traceStack(representation.innerTraceStack.peek()).build()
            vertx.eventBus().publish(portal.portalUuid + "-DisplayInnerTraceStack",
                    new JsonObject(Json.encode(innerTraceStackInfo)))
            log.info("Displayed inner trace stack. Stack size: {}", representation.innerTraceStack.peek().size())
        } else if (traceStack && !traceStack.isEmpty()) {
           vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_TRACE_STACK", representation.traceStack)
           log.info("Displayed trace stack for id: {} - Stack size: {}", traceId, traceStack.size())
       }
    }

    private void displaySpanInfo(SourcePortal portal) {
        def traceId = portal.portalUI.tracesView.traceId
        def spanId = portal.portalUI.tracesView.spanId
        def representation = portal.portalUI.tracesView
        def traceStack
        if (representation.innerTrace) {
            traceStack = representation.innerTraceStack.peek()
        } else {
            traceStack = representation.getTraceStack(traceId)
        }

        for (int i = 0; i < traceStack.size(); i++) {
            def span = traceStack.getJsonObject(i).getJsonObject("span")
            if (span.getInteger("span_id") == spanId) {
                def spanArtifactQualifiedName = span.getString("artifact_qualified_name")
                if (portal.external && span.getBoolean("has_child_stack")) {
                    def spanStackQuery = TraceSpanStackQuery.builder()
                            .oneLevelDeep(true).followExit(true)
                            .segmentId(span.getString("segment_id"))
                            .spanId(span.getLong("span_id"))
                            .traceId(traceId).build()
                    SourcePortalConfig.current.getCoreClient(portal.appUuid).getTraceSpans(portal.appUuid,
                            portal.portalUI.viewingPortalArtifact, spanStackQuery, {
                        if (it.failed()) {
                            log.error("Failed to get trace spans", it.cause())
                            vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_SPAN_INFO", span)
                        } else {
                            def queryResult = it.result()
                            def spanTracesView = portal.portalUI.tracesView
                            spanTracesView.viewType = TracesView.ViewType.TRACE_STACK
                            spanTracesView.innerTraceStack.push(handleTraceStack(
                                    portal.appUuid, portal.portalUI.viewingPortalArtifact, queryResult))

                            displayTraceStack(portal)
                        }
                    })
                    break
                } else if (spanArtifactQualifiedName == null ||
                        spanArtifactQualifiedName == portal.portalUI.viewingPortalArtifact) {
                    vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_SPAN_INFO", span)
                    log.info("Displayed trace span info: {}", span)
                } else {
                    vertx.eventBus().request(CAN_NAVIGATE_TO_ARTIFACT.address, new JsonObject()
                            .put("app_uuid", portal.appUuid)
                            .put("artifact_qualified_name", spanArtifactQualifiedName), {
                        if (it.succeeded() && it.result().body() == true) {
                            def spanStackQuery = TraceSpanStackQuery.builder()
                                    .oneLevelDeep(true).followExit(true)
                                    .segmentId(span.getString("segment_id"))
                                    .spanId(span.getLong("span_id"))
                                    .traceId(traceId).build()

                            def spanPortal = SourcePortal.getInternalPortal(portal.appUuid, spanArtifactQualifiedName)
                            if (!spanPortal.isPresent()) {
                                log.error("Failed to get span portal: {}", spanArtifactQualifiedName)
                                vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_SPAN_INFO", span)
                                return
                            }

                            //todo: cache
                            SourcePortalConfig.current.getCoreClient(portal.appUuid).getTraceSpans(portal.appUuid,
                                    portal.portalUI.viewingPortalArtifact, spanStackQuery, {
                                if (it.failed()) {
                                    log.error("Failed to get trace spans", it.cause())
                                    vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_SPAN_INFO", span)
                                } else {
                                    //navigated away from portal; reset to trace stack
                                    portal.portalUI.tracesView.viewType = TracesView.ViewType.TRACE_STACK

                                    def queryResult = it.result()
                                    def spanTracesView = spanPortal.get().portalUI.tracesView
                                    spanTracesView.viewType = TracesView.ViewType.TRACE_STACK
                                    spanTracesView.innerTraceStack.push(handleTraceStack(
                                            portal.appUuid, portal.portalUI.viewingPortalArtifact, queryResult))
                                    vertx.eventBus().send(NAVIGATE_TO_ARTIFACT.address,
                                            new JsonObject().put("portal_uuid", spanPortal.get().portalUuid)
                                                    .put("artifact_qualified_name", spanArtifactQualifiedName))
                                }
                            })
                        } else {
                            vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_SPAN_INFO", span)
                            log.info("Displayed trace span info: {}", span)
                        }
                    })
                }
            }
        }
    }

    private void handleArtifactTraceResult(ArtifactTraceResult artifactTraceResult) {
        handleArtifactTraceResult(SourcePortal.getPortals(artifactTraceResult.appUuid(),
                artifactTraceResult.artifactQualifiedName()).collect(), artifactTraceResult)
    }

    private void handleArtifactTraceResult(List<SourcePortal> portals, ArtifactTraceResult artifactTraceResult) {
        def traces = new ArrayList<Trace>()
        artifactTraceResult.traces().each {
            traces.add(it.withPrettyDuration(humanReadableDuration(Duration.ofMillis(it.duration()))))
        }
        artifactTraceResult = artifactTraceResult.withTraces(traces)
                .withArtifactSimpleName(removePackageAndClassName(removePackageNames(artifactTraceResult.artifactQualifiedName())))

        portals.each {
            def representation = it.portalUI.tracesView
            representation.cacheArtifactTraceResult(artifactTraceResult)

            if (it.portalUI.viewingPortalArtifact == artifactTraceResult.artifactQualifiedName()
                    && it.portalUI.tracesView.viewType == TracesView.ViewType.TRACES) {
                updateUI(it)
            }
        }
    }

    private static JsonArray handleTraceStack(String appUuid, String rootArtifactQualifiedName,
                                              TraceSpanStackQueryResult spanQueryResult) {
        def spanInfos = new ArrayList<TraceSpanInfo>()
        def totalTime = spanQueryResult.traceSpans().get(0).endTime() - spanQueryResult.traceSpans().get(0).startTime()

        for (def span : spanQueryResult.traceSpans()) {
            def timeTookMs = span.endTime() - span.startTime()
            def timeTook = humanReadableDuration(Duration.ofMillis(timeTookMs))
            def spanInfo = TraceSpanInfo.builder()
                    .span(span)
                    .appUuid(appUuid)
                    .rootArtifactQualifiedName(rootArtifactQualifiedName)
                    .timeTook(timeTook)
                    .totalTracePercent((totalTime == 0) ? 0d : timeTookMs / totalTime * 100.0d)

            //detect if operation name is really an artifact name
            if (QUALIFIED_NAME_PATTERN.matcher(span.endpointName()).matches()) {
                spanInfo.span(span = span.withArtifactQualifiedName(span.endpointName()))
            }
            if (span.artifactQualifiedName()) {
                spanInfo.operationName(removePackageAndClassName(removePackageNames(span.artifactQualifiedName())))
            } else {
                spanInfo.operationName(span.endpointName())
            }
            spanInfos.add(spanInfo.build())
        }
        return new JsonArray(Json.encode(spanInfos))
    }

    static String humanReadableDuration(Duration duration) {
        if (duration.seconds < 1) {
            return duration.toMillis() + "ms"
        }
        return duration.toString().substring(2)
                .replaceAll('(\\d[HMS])(?!$)', '$1 ')
                .toLowerCase()
    }
}
