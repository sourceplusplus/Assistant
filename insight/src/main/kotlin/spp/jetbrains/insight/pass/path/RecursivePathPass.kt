/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022 CodeBrig, Inc.
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
package spp.jetbrains.insight.pass.path

import spp.jetbrains.insight.RuntimePath
import spp.jetbrains.insight.pass.RuntimePathPass
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.marker.model.CallArtifact
import spp.jetbrains.marker.model.FunctionArtifact
import spp.protocol.insight.InsightType.PATH_IS_RECURSIVE
import spp.protocol.insight.InsightValue

/**
 * Adds the [PATH_IS_RECURSIVE] insight to the [RuntimePath] if it is recursive.
 */
class RecursivePathPass : RuntimePathPass {

    override fun analyze(path: RuntimePath) {
        if (path.rootArtifact is FunctionArtifact) {
            //search for calls to the root function
            val calls = path.filterIsInstance<CallArtifact>()
            val recursiveCalls = calls.filter { it.getResolvedFunction() == path.rootArtifact }
            if (recursiveCalls.isNotEmpty()) {
                path.insights.add(InsightValue.of(PATH_IS_RECURSIVE, true))
                recursiveCalls.forEach { it.data[SourceMarkerKeys.RECURSIVE_CALL] = true }
            }
        }
    }
}
