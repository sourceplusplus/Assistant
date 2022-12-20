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
package spp.jetbrains.insight.pass.pathset

import spp.jetbrains.insight.RuntimePath
import spp.jetbrains.insight.pass.RuntimePathSetPass
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.marker.model.IfArtifact

/**
 * Removes paths caused by conditional branches that are always or never taken.
 */
class SimplifyPathSetPass : RuntimePathSetPass {

    /**
     * For each [IfArtifact], analyze if the condition can be statically determined to always be true or false.
     * If so,
     */
    override fun postProcess(pathSet: Set<RuntimePath>): Set<RuntimePath> {
        val simplifiedPaths = mutableSetOf<RuntimePath>()
        for (path in pathSet) {
            //todo: smarter, more dynamic
            if (path.artifacts.first() is IfArtifact) {
                val ifArtifact = path.artifacts.first() as IfArtifact
                val probability = ifArtifact.getData(SourceMarkerKeys.PATH_EXECUTION_PROBABILITY)
                if (probability?.value == 0.0) continue
            }

            simplifiedPaths.add(path)
        }
        return simplifiedPaths
    }
}