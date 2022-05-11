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
package spp.jetbrains.sourcemarker.command

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiDocumentManager
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import liveplugin.implementation.common.toFilePath
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.SourceMarker.creationService
import spp.jetbrains.marker.extend.LiveCommandContext
import spp.jetbrains.marker.jvm.psi.EndpointDetector
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.component.swing.SwingSourceMarkComponentProvider
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode.*
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.inlay.ExpressionInlayMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.sourcemarker.ControlBar
import spp.jetbrains.sourcemarker.command.LiveControlCommand.Companion.CLEAR_LIVE_BREAKPOINTS
import spp.jetbrains.sourcemarker.command.LiveControlCommand.Companion.CLEAR_LIVE_INSTRUMENTS
import spp.jetbrains.sourcemarker.command.LiveControlCommand.Companion.CLEAR_LIVE_LOGS
import spp.jetbrains.sourcemarker.command.LiveControlCommand.Companion.CLEAR_LIVE_METERS
import spp.jetbrains.sourcemarker.command.LiveControlCommand.Companion.CLEAR_LIVE_SPANS
import spp.jetbrains.sourcemarker.command.LiveControlCommand.Companion.HIDE_QUICK_STATS
import spp.jetbrains.sourcemarker.command.LiveControlCommand.Companion.SHOW_QUICK_STATS
import spp.jetbrains.sourcemarker.command.LiveControlCommand.Companion.WATCH_LOG
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.mark.SourceMarkSearch
import spp.jetbrains.sourcemarker.status.LiveStatusManager
import spp.jetbrains.sourcemarker.view.ActivityQuickStatsIndicator
import spp.protocol.SourceServices
import spp.protocol.instrument.LiveInstrumentType.*
import java.awt.BorderLayout
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ControlBarController {

    private val log = LoggerFactory.getLogger(ControlBarController::class.java)
    private var previousControlBar: InlayMark? = null
    private val availableCommands: MutableList<LiveControlCommand> = mutableListOf()

    fun clearAvailableCommands() {
        availableCommands.clear()
    }

    private suspend fun syncAvailableCommands() {
        availableCommands.clear()

        val selfInfo = SourceServices.Instance.liveService!!.getSelf().await()
        availableCommands.addAll(LiveControlCommand.values().toList().filter {
            @Suppress("UselessCallOnCollection") //unknown enums are null
            selfInfo.permissions.filterNotNull().map { it.name }.contains(it.name)
        })

        SourceMarker.commandCenter.getRegisteredLiveCommands().forEach {
            val basePath = it.project.basePath ?: ""
            availableCommands.add(
                LiveControlCommand(
                    it.name,
                    it.name,
                    { it.description },
                    it.selectedIcon?.let {
                        val iconPath = if (File(basePath, it).exists()) {
                            basePath + File.separator + it
                        } else {
                            it
                        }
                        IconLoader.findIcon(File(iconPath).toURL())!!
                    },
                    it.unselectedIcon?.let {
                        val iconPath = if (File(basePath, it).exists()) {
                            basePath + File.separator + it
                        } else {
                            it
                        }
                        IconLoader.findIcon(File(iconPath).toURL())!!
                    },
                    liveCommand = it
                )
            )
            log.info("Registered developer command: ${it.name}")
        }
    }

    private fun determineAvailableCommandsAtLocation(inlayMark: ExpressionInlayMark): List<LiveControlCommand> {
        if (availableCommands.isEmpty()) {
            runBlocking { syncAvailableCommands() }
        }

        val availableCommandsAtLocation = availableCommands.toMutableSet()
        availableCommandsAtLocation.remove(SHOW_QUICK_STATS)

        val parentMark = inlayMark.getParentSourceMark()
        if (parentMark is MethodSourceMark) {
            val loggerDetector = parentMark.getUserData(SourceMarkKeys.LOGGER_DETECTOR)
            if (loggerDetector != null) {
                runBlocking {
                    val detectedLogs = loggerDetector.getOrFindLoggerStatements(parentMark)
                    val logOnCurrentLine = detectedLogs.find { it.lineLocation == inlayMark.lineNumber }
                    if (logOnCurrentLine != null) {
                        availableCommandsAtLocation.add(WATCH_LOG)
                    }
                }
            }

            if (parentMark.getUserData(EndpointDetector.ENDPOINT_ID) != null) {
                val existingQuickStats = parentMark.sourceFileMarker.getSourceMarks().find {
                    it.artifactQualifiedName == parentMark.artifactQualifiedName
                            && it.getUserData(ActivityQuickStatsIndicator.SHOWING_QUICK_STATS) == true
                }
                if (existingQuickStats == null) {
                    availableCommandsAtLocation.add(SHOW_QUICK_STATS)
                } else {
                    availableCommandsAtLocation.add(HIDE_QUICK_STATS)
                }
            }
        }
        return availableCommandsAtLocation.toList()
    }

    fun handleCommandInput(input: String, editor: Editor) {
        log.info("Processing command input: {}", input)
        when (input) {
            SHOW_QUICK_STATS.command -> handleQuickStatsCommand(editor, SHOW_QUICK_STATS)
            HIDE_QUICK_STATS.command -> handleQuickStatsCommand(editor, HIDE_QUICK_STATS)
            WATCH_LOG.command -> {
                //replace command inlay with log status inlay
                val prevCommandBar = previousControlBar!!
                previousControlBar!!.dispose()
                previousControlBar = null

                ApplicationManager.getApplication().runWriteAction {
                    LiveStatusManager.showLogStatusBar(editor, prevCommandBar.lineNumber, true)
                }
            }
            CLEAR_LIVE_BREAKPOINTS.command -> {
                previousControlBar!!.dispose()
                previousControlBar = null

                SourceServices.Instance.liveInstrument!!.clearLiveInstruments(BREAKPOINT).onComplete {
                    if (it.failed()) {
                        log.error("Failed to clear live breakpoints", it.cause())
                    }
                }
            }
            CLEAR_LIVE_LOGS.command -> {
                previousControlBar!!.dispose()
                previousControlBar = null

                SourceServices.Instance.liveInstrument!!.clearLiveInstruments(LOG).onComplete {
                    if (it.failed()) {
                        log.error("Failed to clear live logs", it.cause())
                    }
                }
            }
            CLEAR_LIVE_METERS.command -> {
                previousControlBar!!.dispose()
                previousControlBar = null

                SourceServices.Instance.liveInstrument!!.clearLiveInstruments(METER).onComplete {
                    if (it.failed()) {
                        log.error("Failed to clear live meters", it.cause())
                    }
                }
            }
            CLEAR_LIVE_SPANS.command -> {
                previousControlBar!!.dispose()
                previousControlBar = null

                SourceServices.Instance.liveInstrument!!.clearLiveInstruments(SPAN).onComplete {
                    if (it.failed()) {
                        log.error("Failed to clear live spans", it.cause())
                    }
                }
            }
            CLEAR_LIVE_INSTRUMENTS.command -> {
                previousControlBar!!.dispose()
                previousControlBar = null

                SourceServices.Instance.liveInstrument!!.clearLiveInstruments(null).onComplete {
                    if (it.failed()) {
                        log.error("Failed to clear live instruments", it.cause())
                    }
                }
            }
            else -> {
                availableCommands.find { it.name == input }?.let {
                    val prevCommandBar = previousControlBar!!
                    previousControlBar!!.dispose()
                    previousControlBar = null

                    val sourceMark = SourceMarkSearch.getClosestSourceMark(prevCommandBar.sourceFileMarker, editor)
                    val context = LiveCommandContext(
                        prevCommandBar.sourceFileMarker.psiFile.virtualFile.toFilePath().toFile(),
                        prevCommandBar.lineNumber,
                        prevCommandBar.artifactQualifiedName,
                        sourceMark?.artifactQualifiedName
                    ) { event ->
                        log.debug("Received developer command event: $event")
                        sourceMark?.let {
                            log.info("Triggering developer command event: $event - Source mark: $it")
                            val eventCode = SourceMarkEventCode.fromName(event[0].toString())!!
                            val convertedParams = (event[1] as List<Any?>).map {
                                when {
                                    it == null -> null
                                    it::class.java.name.matches(Regex("^spp..+SourceMarkEventCode$")) ->
                                        SourceMarkEventCode.fromName(it.toString())
                                    else -> it
                                }
                            }
                            it.triggerEvent(eventCode, convertedParams, event[2] as (() -> Unit)?)
                        }
                    }
                    sourceMark?.getUserData()?.forEach {
                        context.putUserData((it.key as SourceKey<*>).name, it.value)
                    }
                    it.liveCommand?.trigger(context)
                }
            }
        }
    }

    private fun handleQuickStatsCommand(editor: Editor, command: LiveControlCommand) {
        val sourceMark = SourceMarkSearch.getClosestSourceMark(previousControlBar!!.sourceFileMarker, editor)
        if (sourceMark != null) {
            sourceMark.triggerEvent(CUSTOM_EVENT, listOf(command))
        } else {
            log.warn("No source mark found for command: {}", command)
        }

        previousControlBar!!.dispose()
        previousControlBar = null
    }

    fun canShowControlBar(fileMarker: SourceFileMarker, lineNumber: Int): Boolean {
        return creationService.getOrCreateExpressionInlayMark(fileMarker, lineNumber).isPresent
    }

    /**
     * Attempts to display live control bar below [lineNumber].
     */
    fun showControlBar(editor: Editor, lineNumber: Int, tryingAboveLine: Boolean = false) {
        //close previous control bar (if open)
        previousControlBar?.dispose(true, false)
        previousControlBar = null

        //determine control bar location
        val fileMarker = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)!!
            .getUserData(SourceFileMarker.KEY)
        if (fileMarker == null) {
            log.warn("Could not find file marker for file: {}", editor.document)
            return
        }

        val findInlayMark = creationService.getOrCreateExpressionInlayMark(fileMarker, lineNumber)
        if (findInlayMark.isPresent) {
            val inlayMark = findInlayMark.get()
            if (fileMarker.containsSourceMark(inlayMark)) {
                if (!tryingAboveLine) {
                    //already showing inlay here, try line above
                    showControlBar(editor, lineNumber - 1, true)
                }
            } else {
                //create and display control bar
                previousControlBar = inlayMark

                val wrapperPanel = JPanel()
                wrapperPanel.layout = BorderLayout()

                val controlBar = ControlBar(editor, inlayMark, determineAvailableCommandsAtLocation(inlayMark))
                wrapperPanel.add(controlBar)
                editor.scrollingModel.addVisibleAreaListener(controlBar)

                inlayMark.configuration.showComponentInlay = true
                inlayMark.configuration.componentProvider = object : SwingSourceMarkComponentProvider() {
                    override fun makeSwingComponent(sourceMark: SourceMark): JComponent = wrapperPanel
                }
                inlayMark.visible.set(true)
                inlayMark.apply()

                controlBar.focus()
            }
        } else if (tryingAboveLine) {
            log.warn("No detected expression at line {}. Inlay mark ignored", lineNumber)
        } else {
            showControlBar(editor, lineNumber - 1, true)
        }
    }
}
