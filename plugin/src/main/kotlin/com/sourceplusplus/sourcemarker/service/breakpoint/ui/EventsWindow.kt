package com.sourceplusplus.sourcemarker.service.breakpoint.ui

import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.sourceplusplus.sourcemarker.service.breakpoint.LiveBreakpointConstants
import com.sourceplusplus.sourcemarker.service.breakpoint.DebugStackFrameListener
import com.sourceplusplus.sourcemarker.service.breakpoint.StackFrameManager
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.JComponent

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class EventsWindow(project: Project) : Disposable {

    lateinit var stackFrameManager: StackFrameManager
    private val listeners: MutableList<DebugStackFrameListener>
    private val layoutUi: RunnerLayoutUi
    lateinit var eventsTab: EventsTab

    private fun addEventsTab() {
        eventsTab = EventsTab()
        val content = layoutUi.createContent(
            LiveBreakpointConstants.LIVE_RECORDER_STACK_FRAMES, eventsTab.component, "Events",
            AllIcons.Debugger.Console, null
        )
        content.isCloseable = false
        layoutUi.addContent(content, 0, PlaceInGrid.left, false)
        Disposer.register(this, eventsTab)
    }

    val layoutComponent: JComponent
        get() = layoutUi.component

    override fun dispose() {}

    init {
        listeners = CopyOnWriteArrayList()
        layoutUi = RunnerLayoutUi.Factory.getInstance(project).create(
            LiveBreakpointConstants.LIVE_RUNNER,
            LiveBreakpointConstants.LIVE_RUNNER,
            LiveBreakpointConstants.LIVE_RUNNER,
            this
        )
        addEventsTab()
    }
}
