package com.sourceplusplus.sourcemarker.service.log

import com.intellij.util.ui.ColumnInfo
import com.sourceplusplus.protocol.instrument.LiveInstrumentEvent
import com.sourceplusplus.protocol.instrument.LiveInstrumentEventType
import com.sourceplusplus.protocol.instrument.log.event.LiveLogHit
import com.sourceplusplus.protocol.instrument.log.event.LiveLogRemoved
import com.sourceplusplus.protocol.utils.toPrettyDuration
import com.sourceplusplus.sourcemarker.PluginBundle.message
import io.vertx.core.json.Json
import kotlinx.datetime.Clock

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LogHitColumnInfo(name: String) : ColumnInfo<LiveInstrumentEvent, String>(name) {

    override fun getComparator(): Comparator<LiveInstrumentEvent>? {
        return when (name) {
            "Time" -> Comparator { t: LiveInstrumentEvent, t2: LiveInstrumentEvent ->
                val obj1 = if (t.eventType == LiveInstrumentEventType.LOG_HIT) {
                    Json.decodeValue(t.data, LiveLogHit::class.java)
                } else if (t.eventType == LiveInstrumentEventType.LOG_REMOVED) {
                    Json.decodeValue(t.data, LiveLogRemoved::class.java)
                } else {
                    throw IllegalArgumentException(t.eventType.name)
                }
                val obj2 = if (t2.eventType == LiveInstrumentEventType.LOG_HIT) {
                    Json.decodeValue(t2.data, LiveLogHit::class.java)
                } else if (t2.eventType == LiveInstrumentEventType.LOG_REMOVED) {
                    Json.decodeValue(t2.data, LiveLogRemoved::class.java)
                } else {
                    throw IllegalArgumentException(t2.eventType.name)
                }
                obj1.occurredAt.compareTo(obj2.occurredAt)
            }
            else -> null
        }
    }

    override fun valueOf(event: LiveInstrumentEvent): String {
        if (event.eventType == LiveInstrumentEventType.LOG_HIT) {
            val item = Json.decodeValue(event.data, LiveLogHit::class.java)
            return when (name) {
                "Message" -> item.logResult.logs.first().getFormattedMessage()
                "Time" ->
                    (Clock.System.now().toEpochMilliseconds() - item.occurredAt.toEpochMilliseconds())
                        .toPrettyDuration() + " " + message("ago")
                else -> item.toString()
            }
        } else {
            val item = Json.decodeValue(event.data, LiveLogRemoved::class.java)
            return when (name) {
                "Message" -> item.cause!!.message!!
                "Time" -> (Clock.System.now().toEpochMilliseconds() - item.occurredAt.toEpochMilliseconds())
                    .toPrettyDuration() + " " + message("ago")
                else -> item.toString()
            }
        }
    }
}
