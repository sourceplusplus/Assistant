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
        val portalServer = PortalServer()
        vertx.deployVerticle(portalServer).await()
        log.info("Portal server initialized")

        val initialUrl = AtomicReference("")
        val componentProvider = SourceMarkSingleJcefComponentProvider().apply {
            defaultConfiguration.browserLoadingListener = object: BrowserLoadingListener() {
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
