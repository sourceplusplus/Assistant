package spp.jetbrains.sourcemarker.service.breakpoint

import com.intellij.util.ui.ColumnInfo
import spp.protocol.instrument.LiveInstrumentEvent
import spp.protocol.instrument.LiveInstrumentEventType
import spp.protocol.instrument.breakpoint.event.LiveBreakpointHit
import spp.protocol.instrument.breakpoint.event.LiveBreakpointRemoved
import spp.protocol.utils.toPrettyDuration
import spp.jetbrains.sourcemarker.PluginBundle.message
import io.vertx.core.json.Json
import kotlinx.datetime.Clock

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class BreakpointHitColumnInfo(name: String) : ColumnInfo<LiveInstrumentEvent, String>(name) {

    override fun getComparator(): Comparator<LiveInstrumentEvent>? {
        return when (name) {
            "Time" -> Comparator { t: LiveInstrumentEvent, t2: LiveInstrumentEvent ->
                val obj1 = if (t.eventType == LiveInstrumentEventType.BREAKPOINT_HIT) {
                    Json.decodeValue(t.data, LiveBreakpointHit::class.java)
                } else if (t.eventType == LiveInstrumentEventType.BREAKPOINT_REMOVED) {
                    Json.decodeValue(t.data, LiveBreakpointRemoved::class.java)
                } else {
                    throw IllegalArgumentException(t.eventType.name)
                }
                val obj2 = if (t2.eventType == LiveInstrumentEventType.BREAKPOINT_HIT) {
                    Json.decodeValue(t2.data, LiveBreakpointHit::class.java)
                } else if (t2.eventType == LiveInstrumentEventType.BREAKPOINT_REMOVED) {
                    Json.decodeValue(t2.data, LiveBreakpointRemoved::class.java)
                } else {
                    throw IllegalArgumentException(t2.eventType.name)
                }
                obj1.occurredAt.compareTo(obj2.occurredAt)
            }
            else -> null
        }
    }

    override fun valueOf(event: LiveInstrumentEvent): String {
        val breakpointData = mutableListOf<Map<String, Any>>()
        if (event.eventType == LiveInstrumentEventType.BREAKPOINT_HIT) {
            val item = Json.decodeValue(event.data, LiveBreakpointHit::class.java)
            item.stackTrace.elements.first().variables.forEach {
                breakpointData.add(mapOf(it.name to it.value))
            }
            return when (name) {
                "Breakpoint Data" -> Json.encode(breakpointData)
                "Time" ->
                    (Clock.System.now().toEpochMilliseconds() - item.occurredAt.toEpochMilliseconds())
                        .toPrettyDuration() + " " + message("ago")
                else -> item.toString()
            }
        } else {
            val item = Json.decodeValue(event.data, LiveBreakpointRemoved::class.java)
            return when (name) {
                "Breakpoint Data" -> item.cause!!.message ?: item.cause!!.exceptionType
                "Time" -> (Clock.System.now().toEpochMilliseconds() - item.occurredAt.toEpochMilliseconds())
                    .toPrettyDuration() + " " + message("ago")
                else -> item.toString()
            }
        }
    }
}