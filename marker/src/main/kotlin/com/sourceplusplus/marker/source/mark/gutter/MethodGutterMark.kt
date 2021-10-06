package com.sourceplusplus.marker.source.mark.gutter

import com.intellij.psi.PsiNameIdentifierOwner
import com.sourceplusplus.marker.plugin.SourceMarkerVisibilityAction
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.gutter.config.GutterMarkConfiguration
import com.sourceplusplus.marker.source.mark.gutter.event.GutterMarkEventCode
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import com.sourceplusplus.marker.SourceMarker.configuration as pluginConfiguration

/**
 * Represents a [GutterMark] associated to a method artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class MethodGutterMark(
    override val sourceFileMarker: SourceFileMarker,
    override var psiMethod: PsiNameIdentifierOwner
) : MethodSourceMark(sourceFileMarker, psiMethod), GutterMark {

    override val id: String = UUID.randomUUID().toString()
    override val configuration: GutterMarkConfiguration = pluginConfiguration.gutterMarkConfiguration.copy()
    private var visible: AtomicBoolean = AtomicBoolean(SourceMarkerVisibilityAction.globalVisibility)

    override fun isVisible(): Boolean {
        return visible.get()
    }

    override fun setVisible(visible: Boolean) {
        val previousVisibility = this.visible.getAndSet(visible)
        if (visible && !previousVisibility) {
            triggerEvent(SourceMarkEvent(this, GutterMarkEventCode.GUTTER_MARK_VISIBLE))
        } else if (!visible && previousVisibility) {
            triggerEvent(SourceMarkEvent(this, GutterMarkEventCode.GUTTER_MARK_HIDDEN))
        }
    }
}
