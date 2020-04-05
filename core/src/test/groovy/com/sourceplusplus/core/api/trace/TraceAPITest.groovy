package com.sourceplusplus.core.api.trace

import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.config.SourceAgentConfig
import com.sourceplusplus.api.model.trace.ArtifactTraceSubscribeRequest
import com.sourceplusplus.api.model.trace.TraceOrderType
import com.sourceplusplus.api.model.trace.TraceQuery
import com.sourceplusplus.api.model.trace.TraceSpanStackQuery
import com.sourceplusplus.core.api.SourceCoreAPITest
import groovy.util.logging.Slf4j
import io.vertx.ext.unit.TestSuite
import org.junit.Before
import org.junit.Test
import test.integration.trace.TraceTest

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * @version 0.2.6
 * @since 0.2.6
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class TraceAPITest extends SourceCoreAPITest {

    @Before
    void resetAgentAndApacheSkywalking() {
        log.info("Resetting Source++ and Apache SkyWalking integration")
        def skywalkingPackage = "org.apache.skywalking.apm.agent.core"
        def serviceManagerInstance = skywalkingPackage + ".boot.ServiceManager.INSTANCE"
        def downstreamAgentConfig = skywalkingPackage + ".conf.RemoteDownstreamConfig.Agent"
        def serviceRegisterClientClass = skywalkingPackage + ".remote.ServiceAndEndpointRegisterClient.class"
        def commandExecutorServiceClass = skywalkingPackage + ".commands.CommandExecutorService.class"
        Eval.me(serviceManagerInstance + ".findService($commandExecutorServiceClass)")
                ."commandExecutorMap"["ServiceMetadataReset"]."execute"()
        SourceAgentConfig.current.appUuid = UUID.randomUUID().toString()
        Eval.me(skywalkingPackage + ".conf.Config.Agent.SERVICE_NAME = '" + SourceAgentConfig.current.appUuid + "'")
        Eval.me(serviceManagerInstance + ".findService($serviceRegisterClientClass)")."coolDownStartTime" = -1
        Eval.me(serviceManagerInstance + ".findService($serviceRegisterClientClass)")."run"()
        while (Eval.me(downstreamAgentConfig + ".SERVICE_ID") == 0
                || Eval.me(downstreamAgentConfig + ".SERVICE_INSTANCE_ID") == 0) {
            try {
                Thread.sleep(100)
            } catch (InterruptedException ignore) {
            }
        }
        log.info("Reset Source++ and Apache SkyWalking integration")
    }

    @Test
    void testCallDepth1() {
        SourceApplication application
        TestSuite.create("testCallDepth1-setup").before({ test ->
            def async = test.async(1)
            coreClient.createApplication(SourceApplication.builder().isCreateRequest(true)
                    .appUuid(SourceAgentConfig.current.appUuid).build(), {
                if (it.failed()) {
                    test.fail(it.cause())
                }

                application = it.result()
                async.countDown()
            })
        }).test("testCallDepth1-check_trace_data", { test ->
            def async = test.async()
            def traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
                    .addOrderTypes(TraceOrderType.LATEST_TRACES)
                    .appUuid(application.appUuid())
                    .artifactQualifiedName("test.integration.trace.TraceTest.threeStaticMethodCallDepth()").build()
            coreClient.subscribeToArtifact(traceSubscribeRequest, {
                if (it.failed()) {
                    test.fail(it.cause())
                }

                for (int i = 0; i < 40; i++) {
                    TraceTest.threeStaticMethodCallDepth()
                }

                def traceQuery = TraceQuery.builder()
                        .appUuid(application.appUuid())
                        .artifactQualifiedName("test.integration.trace.TraceTest.threeStaticMethodCallDepth()")
                        .orderType(TraceOrderType.LATEST_TRACES)
                        .pageSize(10)
                        .durationStart(Instant.now().minus(14, ChronoUnit.MINUTES))
                        .durationStop(Instant.now())
                        .durationStep("SECOND").build()
                coreClient.getTraces(traceQuery, {
                    if (it.failed()) {
                        test.fail(it.cause())
                    }

                    test.assertEquals(10, it.result().traces().size())
                    it.result().traces().each {
                        test.assertEquals(1, it.operationNames().size())
                        test.assertEquals(1, it.traceIds().size())
                        test.assertEquals(false, it.error)
                        test.assertEquals("test.integration.trace.TraceTest.threeStaticMethodCallDepth()", it.operationNames()[0])
                        test.assertInRange(500, it.duration(), 100)
                    }

                    def queryTraceId = it.result().traces()[0].traceIds()[0]
                    def spanStackQuery = TraceSpanStackQuery.builder()
                            .oneLevelDeep(true)
                            .traceId(queryTraceId).build()
                    coreClient.getTraceSpans(application.appUuid(),
                            "test.integration.trace.TraceTest.threeStaticMethodCallDepth()",
                            spanStackQuery, {
                        if (it.failed()) {
                            test.fail(it.cause())
                        }

                        test.assertEquals(1, it.result().total())
                        test.assertEquals(1, it.result().traceSpans().size())
                        def traceSpan = it.result().traceSpans()[0]
                        test.assertEquals(false, traceSpan.error)
                        test.assertEquals(-1L, traceSpan.parentSpanId())
                        test.assertEquals(0L, traceSpan.spanId())
                        test.assertEquals(application.appUuid(), traceSpan.serviceCode())
                        test.assertEquals("test.integration.trace.TraceTest.threeStaticMethodCallDepth()", traceSpan.endpointName())
                        test.assertEquals("test.integration.trace.TraceTest.threeStaticMethodCallDepth()", traceSpan.artifactQualifiedName())
                        test.assertEquals("Entry", traceSpan.type())

                        async.countDown()
                    })
                })
            })
        }).run().awaitSuccess()
    }

    @Test
    void testCallDepth2() {
        SourceApplication application
        TestSuite.create("testCallDepth2-setup").before({ test ->
            def async = test.async(1)
            coreClient.createApplication(SourceApplication.builder().isCreateRequest(true)
                    .appUuid(SourceAgentConfig.current.appUuid).build(), {
                if (it.failed()) {
                    test.fail(it.cause())
                }

                application = it.result()
                async.countDown()
            })
        }).test("testCallDepth2-check_trace_data", { test ->
            def async = test.async()
            def traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
                    .addOrderTypes(TraceOrderType.LATEST_TRACES)
                    .appUuid(application.appUuid())
                    .artifactQualifiedName("test.integration.trace.TraceTest.threeStaticMethodCallDepth()").build()
            coreClient.subscribeToArtifact(traceSubscribeRequest, {
                if (it.failed()) {
                    test.fail(it.cause())
                }

                traceSubscribeRequest = ArtifactTraceSubscribeRequest.builder()
                        .addOrderTypes(TraceOrderType.LATEST_TRACES)
                        .appUuid(application.appUuid())
                        .artifactQualifiedName("test.integration.trace.TraceTest.firstMethod()").build()
                coreClient.subscribeToArtifact(traceSubscribeRequest, {
                    if (it.failed()) {
                        test.fail(it.cause())
                    }

                    for (int i = 0; i < 40; i++) {
                        TraceTest.threeStaticMethodCallDepth()
                    }

                    def traceQuery = TraceQuery.builder()
                            .appUuid(application.appUuid())
                            .artifactQualifiedName("test.integration.trace.TraceTest.threeStaticMethodCallDepth()")
                            .orderType(TraceOrderType.LATEST_TRACES)
                            .pageSize(10)
                            .durationStart(Instant.now().minus(14, ChronoUnit.MINUTES))
                            .durationStop(Instant.now())
                            .durationStep("SECOND").build()
                    coreClient.getTraces(traceQuery, {
                        if (it.failed()) {
                            test.fail(it.cause())
                        }

                        test.assertEquals(10, it.result().traces().size())
                        it.result().traces().each {
                            test.assertEquals(1, it.operationNames().size())
                            test.assertEquals(1, it.traceIds().size())
                            test.assertEquals(false, it.error)
                            test.assertEquals("test.integration.trace.TraceTest.threeStaticMethodCallDepth()", it.operationNames()[0])
                            test.assertInRange(500, it.duration(), 100)
                        }

                        def queryTraceId = it.result().traces()[0].traceIds()[0]
                        def spanStackQuery = TraceSpanStackQuery.builder()
                                .oneLevelDeep(true)
                                .traceId(queryTraceId).build()
                        coreClient.getTraceSpans(application.appUuid(),
                                "test.integration.trace.TraceTest.threeStaticMethodCallDepth()",
                                spanStackQuery, {
                            if (it.failed()) {
                                test.fail(it.cause())
                            }

                            test.assertEquals(2, it.result().total())
                            test.assertEquals(2, it.result().traceSpans().size())
                            def rootSpan = it.result().traceSpans()[0]
                            test.assertEquals(false, rootSpan.error)
                            test.assertEquals(-1L, rootSpan.parentSpanId())
                            test.assertEquals(0L, rootSpan.spanId())
                            test.assertEquals(application.appUuid(), rootSpan.serviceCode())
                            test.assertEquals("test.integration.trace.TraceTest.threeStaticMethodCallDepth()", rootSpan.endpointName())
                            test.assertEquals("test.integration.trace.TraceTest.threeStaticMethodCallDepth()", rootSpan.artifactQualifiedName())
                            test.assertEquals("Entry", rootSpan.type())
                            def firstInnerMethod = it.result().traceSpans()[1]
                            test.assertEquals(false, firstInnerMethod.error)
                            test.assertEquals(0L, firstInnerMethod.parentSpanId())
                            test.assertEquals(1L, firstInnerMethod.spanId())
                            test.assertEquals(application.appUuid(), firstInnerMethod.serviceCode())
                            test.assertEquals("test.integration.trace.TraceTest.firstMethod()", firstInnerMethod.endpointName())
                            test.assertEquals(null, firstInnerMethod.artifactQualifiedName())
                            test.assertEquals("Local", firstInnerMethod.type()) //todo: shouldn't this have a value?

                            async.countDown()
                        })
                    })
                })
            })
        }).run().awaitSuccess()
    }
}
