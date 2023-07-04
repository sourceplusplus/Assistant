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
package spp.jetbrains.sourcemarker.view

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import spp.jetbrains.UserData
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.invokeLater
import spp.jetbrains.sourcemarker.view.action.ResumeViewAction
import spp.jetbrains.sourcemarker.view.action.SetRefreshIntervalAction
import spp.jetbrains.sourcemarker.view.action.StopViewAction
import spp.jetbrains.sourcemarker.view.window.LiveLogWindowImpl
import spp.jetbrains.status.SourceStatusService
import spp.jetbrains.view.ResumableView
import spp.jetbrains.view.manager.LiveViewLogManager
import spp.jetbrains.view.window.LiveLogWindow
import spp.protocol.platform.general.Service
import spp.protocol.service.SourceServices.Subscribe.toLiveViewSubscriberAddress
import spp.protocol.view.LiveView
import spp.protocol.view.LiveViewConfig
import spp.protocol.view.LiveViewEvent

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewLogManagerImpl(
    private val project: Project
) : LiveViewLogManager, ContentManagerListener {

    companion object {
        fun init(project: Project) {
            project.getService(LiveViewLogManagerImpl::class.java)
        }
    }

    private val log = logger<LiveViewLogManagerImpl>()
    private val toolWindowId = "Live Logs"
    private val contentFactory = ApplicationManager.getApplication().getService(ContentFactory::class.java)
    private var toolWindow: ToolWindow
    private var contentManager: ContentManager
    override var currentView: ResumableView? = null
    override val refreshInterval: Int?
        get() = currentView?.refreshInterval

    init {
        val existingToolWindow = ToolWindowManager.getInstance(project).getToolWindow(toolWindowId)
        if (existingToolWindow == null) {
            toolWindow = ToolWindowManager.getInstance(project)
                .registerToolWindow(RegisterToolWindowTask.closable(toolWindowId, PluginIcons.ToolWindow.memo))
        } else {
            toolWindow = existingToolWindow
            toolWindow.isAvailable = true
        }
        contentManager = toolWindow.contentManager

        project.putUserData(LiveViewLogManager.KEY, this)
        SourceStatusService.getInstance(project).onReadyChange {
            if (it.isReady) {
                val service = SourceStatusService.getCurrentService(project)
                if (service == null) {
                    log.warn("No service found for project: ${project.name}")
                    return@onReadyChange
                }

                showServicesWindow(service)
            } else {
                project.invokeLater {
                    hideWindows()
                }
            }
        }
        contentManager.addContentManagerListener(this)

        project.messageBus.connect().subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
            override fun stateChanged(toolWindowManager: ToolWindowManager) {
                if (toolWindow.isVisible) {
                    (contentManager.contents.firstOrNull()?.disposer as? ResumableView)?.onFocused()
                }
            }
        })

        Disposer.register(this, contentManager)

        toolWindow.setTitleActions(
            listOf(
                ResumeViewAction(this),
                StopViewAction(this),
                SetRefreshIntervalAction(this)
            )
        )
    }

    override fun selectionChanged(event: ContentManagerEvent) {
        if (event.operation == ContentManagerEvent.ContentOperation.add) {
            currentView = event.content.disposer as ResumableView

            if (toolWindow.isVisible) {
                currentView?.onFocused()
            }
        }
    }

    override fun contentRemoved(event: ContentManagerEvent) {
        val removedWindow = event.content.disposer as ResumableView
        removedWindow.pause()

        if (removedWindow == currentView) {
            currentView = null
        }
    }

    private fun showServicesWindow(service: Service) = project.invokeLater {
        val viewService = UserData.liveViewService(project)
        if (viewService == null) {
            log.warn("LiveViewService not available for project: ${project.name}")
            return@invokeLater
        }

        val liveView = LiveView(
            entityIds = mutableSetOf(service.name),
            viewConfig = LiveViewConfig("SERVICE_LOGS_WINDOW", listOf("service_logs"), 1000)
        )
        val logWindow = LiveLogWindowImpl(project, viewService, liveView, { serviceLogsConsumerCreator(it) })
        val overviewContent = contentFactory.createContent(
            logWindow.component,
            "Service: ${service.name}",
            true
        )
        overviewContent.setDisposer(logWindow)
        overviewContent.isCloseable = false
        contentManager.addContent(overviewContent)
    }

    private fun serviceLogsConsumerCreator(logWindow: LiveLogWindow): MessageConsumer<JsonObject> {
        val vertx = UserData.vertx(project)
        val developerId = UserData.developerId(project)
        val consumer = vertx.eventBus().consumer<JsonObject>(toLiveViewSubscriberAddress(developerId))
        consumer.handler {
            val liveViewEvent = LiveViewEvent(it.body())
            if (liveViewEvent.subscriptionId != logWindow.liveView.subscriptionId) return@handler

            logWindow.handleEvent(liveViewEvent)
        }
        return consumer
    }

    private fun hideWindows() {
        contentManager.contents.forEach { content ->
            contentManager.removeContent(content, true)
        }
    }

    override fun getOrCreateLogWindow(
        liveView: LiveView,
        consumer: (LiveLogWindow) -> MessageConsumer<JsonObject>,
        title: String
    ): LiveLogWindow {
        val existingContent = contentManager.findContent(title)
        if (existingContent != null) {
            project.invokeLater {
                contentManager.setSelectedContent(existingContent)
                toolWindow.show()
            }

            val logWindow = existingContent.disposer as LiveLogWindow
            logWindow.resume()
            return logWindow
        }

        val viewService = UserData.liveViewService(project)
        require(viewService != null) { "LiveViewService not available" }

        val logWindow = LiveLogWindowImpl(project, viewService, liveView, consumer)
        logWindow.resume()

        project.invokeLater {
            val content = contentFactory
                .createContent(logWindow.component, title, false)
            content.setDisposer(logWindow)
            contentManager.addContent(content)
            contentManager.setSelectedContent(content)

            toolWindow.show()
        }

        return logWindow
    }

    override fun dispose() = Unit
}
