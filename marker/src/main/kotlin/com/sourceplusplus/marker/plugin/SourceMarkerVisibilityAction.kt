package com.sourceplusplus.marker.plugin

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.sourceplusplus.marker.SourceMarker

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerVisibilityAction : AnAction() {

    companion object {
        var globalVisibility = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        globalVisibility = !globalVisibility
        val currentMarks = SourceMarker.getSourceMarks()
        if (currentMarks.isNotEmpty()) {
            currentMarks.forEach { it.setVisible(globalVisibility) }
            DaemonCodeAnalyzer.getInstance(e.project).restart()
        }
    }
}
