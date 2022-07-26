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
package spp.jetbrains.sourcemarker.portal

import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory
import spp.booster.PortalServer
import spp.booster.SourcePortal
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.mark.api.component.api.config.ComponentSizeEvaluator
import spp.jetbrains.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration
import spp.jetbrains.marker.source.mark.api.component.jcef.SourceMarkSingleJcefComponentProvider
import spp.jetbrains.marker.source.mark.api.component.jcef.config.BrowserLoadingListener
import spp.jetbrains.marker.source.mark.api.component.jcef.config.SourceMarkJcefComponentConfiguration
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.jetbrains.sourcemarker.settings.getServicePortNormalized
import spp.jetbrains.sourcemarker.settings.isSsl
import spp.jetbrains.sourcemarker.settings.serviceHostNormalized
import spp.protocol.marshall.KSerializers
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicReference
import javax.swing.UIManager
import kotlin.math.ceil
import kotlin.math.floor

class PortalController(private val markerConfig: SourceMarkerConfig) : CoroutineVerticle() {

    private val log = LoggerFactory.getLogger(PortalController::class.java)

    override suspend fun start() {
        log.info("Initializing portal")

        val module = SimpleModule()
        module.addSerializer(Instant::class.java, KSerializers.KotlinInstantSerializer())
        module.addDeserializer(Instant::class.java, KSerializers.KotlinInstantDeserializer())
        DatabindCodec.mapper().registerModule(module)

        log.info("Initializing portal server")
        val portalServer = PortalServer(
            skywalkingHost = markerConfig.serviceHostNormalized,
            skywalkingPort = markerConfig.getServicePortNormalized(),
            ssl = markerConfig.isSsl(),
            jwtToken = markerConfig.serviceToken
        )
        vertx.deployVerticle(portalServer).await()
        log.info("Portal server initialized")

        val initialUrl = AtomicReference("")
        val componentProvider = SourceMarkSingleJcefComponentProvider().apply {
            defaultConfiguration.browserLoadingListener = object : BrowserLoadingListener() {
                override fun beforeBrowserCreated(configuration: SourceMarkJcefComponentConfiguration) {
                    configuration.initialUrl = initialUrl.get()
                }
            }
            defaultConfiguration.zoomLevel = markerConfig.portalConfig.zoomLevel
            defaultConfiguration.componentSizeEvaluator = object : ComponentSizeEvaluator() {
                override fun getDynamicSize(
                    editor: Editor,
                    configuration: SourceMarkComponentConfiguration
                ): Dimension {
                    val widthDouble = 963 * markerConfig.portalConfig.zoomLevel
                    val heightDouble = 350 * markerConfig.portalConfig.zoomLevel
                    var width: Int = widthDouble.toInt()
                    if (ceil(widthDouble) != floor(widthDouble)) {
                        width = ceil(widthDouble).toInt() + 1
                    }
                    var height = heightDouble.toInt()
                    if (ceil(heightDouble) != floor(heightDouble)) {
                        height = ceil(heightDouble).toInt() + 1
                    }
                    return Dimension(width, height)
                }
            }
        }
        SourceMarker.configuration.guideMarkConfiguration.componentProvider = componentProvider
        SourceMarker.configuration.inlayMarkConfiguration.componentProvider = componentProvider

        SourceMarker.addGlobalSourceMarkEventListener {
            if (it.eventCode == SourceMarkEventCode.MARK_BEFORE_ADDED && it.sourceMark is GuideMark) {
                //register portal for source mark
                val portal = SourcePortal.getPortal(
                    SourcePortal.register(it.sourceMark.artifactQualifiedName, false)
                )!!
                it.sourceMark.putUserData(SourceMarkKeys.PORTAL_CONFIGURATION, portal.configuration)

                it.sourceMark.addEventListener {
                    if (it.eventCode == SourceMarkEventCode.UPDATE_PORTAL_CONFIG) {
                        if (it.params.first() is String && it.params.first() == "setPage") {
                            initialUrl.set("http://localhost:${portalServer.serverPort}${it.params.get(1)}")
                            vertx.eventBus().publish(
                                "portal.SetCurrentPage",
                                JsonObject().put("page", it.params.get(1) as String)
                            )
                        }
                    } else if (it.eventCode == SourceMarkEventCode.PORTAL_OPENING) {
                        portal.configuration.darkMode = UIManager.getLookAndFeel() !is IntelliJLaf
                        portal.configuration.config["portal_uuid"] = portal.portalUuid
                        ApplicationManager.getApplication().invokeLater(it.sourceMark::displayPopup)
                    }
                }
            }
        }

        log.info("Portal initialized")
    }
}
