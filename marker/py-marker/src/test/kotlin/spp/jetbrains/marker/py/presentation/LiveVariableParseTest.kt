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
package spp.jetbrains.marker.py.presentation

import com.google.common.io.Resources
import io.vertx.core.json.JsonObject
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import spp.protocol.instrument.event.LiveBreakpointHit

class LiveVariableParseTest {

    @Test
    fun testDictParse() {
        val bpHitJson = Resources.getResource("breakpointHit/dictParse.json").readText()
        val bpHit = LiveBreakpointHit(JsonObject(bpHitJson))
        val builtInsVar = bpHit.stackTrace.elements.first().variables.find { it.name == "__builtins__" }
        assertNotNull(builtInsVar)

        val parsedDict = PythonVariableNode.parseDict(builtInsVar!!.value as String)
        assertNotNull(parsedDict)
        assertEquals(152, parsedDict.size)
    }
}
