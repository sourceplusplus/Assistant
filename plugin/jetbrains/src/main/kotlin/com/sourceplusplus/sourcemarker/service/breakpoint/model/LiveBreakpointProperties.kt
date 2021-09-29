package com.sourceplusplus.sourcemarker.service.breakpoint.model

import com.intellij.util.xml.Attribute
import com.sourceplusplus.protocol.instrument.LiveSourceLocation
import org.jetbrains.java.debugger.breakpoints.properties.JavaBreakpointProperties

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveBreakpointProperties : JavaBreakpointProperties<LiveBreakpointProperties>() {

    private var suspend: Boolean = false
    private var active: Boolean = false
    private var finished: Boolean = false
    private var location: LiveSourceLocation? = null
    private var breakpointId: String? = null
    private var breakpointCondition: String? = null

    @Attribute("suspend")
    fun getSuspend(): Boolean {
        return suspend
    }

    @Attribute("suspend")
    fun setSuspend(suspend: Boolean) {
        this.suspend = suspend
    }

    @Attribute("active")
    fun getActive(): Boolean {
        return active
    }

    @Attribute("active")
    fun setActive(active: Boolean) {
        this.active = active
    }

    @Attribute("finished")
    fun getFinished(): Boolean {
        return finished
    }

    @Attribute("finished")
    fun setFinished(finished: Boolean) {
        this.finished = finished
    }

    @Attribute("location")
    fun getLocation(): LiveSourceLocation? {
        return location
    }

    @Attribute("location")
    fun setLocation(location: LiveSourceLocation) {
        this.location = location
    }

    @Attribute("breakpointId")
    fun getBreakpointId(): String? {
        return breakpointId
    }

    @Attribute("breakpointId")
    fun setBreakpointId(breakpointId: String) {
        this.breakpointId = breakpointId
    }

    @Attribute("breakpointCondition")
    fun getBreakpointCondition(): String? {
        return breakpointCondition
    }

    @Attribute("breakpointCondition")
    fun setBreakpointCondition(breakpointCondition: String) {
        this.breakpointCondition = breakpointCondition
    }

    override fun getState(): LiveBreakpointProperties? {
        return super.getState()
    }

    override fun loadState(state: LiveBreakpointProperties) {
        super.loadState(state)

        suspend = state.suspend
        active = state.active
        finished = state.finished
        location = state.location
        breakpointId = state.breakpointId
        breakpointCondition = state.breakpointCondition
    }
}
