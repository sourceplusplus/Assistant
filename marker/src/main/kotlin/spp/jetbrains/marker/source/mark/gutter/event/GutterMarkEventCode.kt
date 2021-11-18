package spp.jetbrains.marker.source.mark.gutter.event

import spp.jetbrains.marker.source.mark.api.event.IEventCode
import spp.jetbrains.marker.source.mark.gutter.GutterMark

/**
 * Represents [GutterMark]-specific events.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
enum class GutterMarkEventCode(private val code: Int) : IEventCode {
    GUTTER_MARK_VISIBLE(2000),
    GUTTER_MARK_HIDDEN(2001);

    override fun code(): Int {
        return this.code
    }
}
