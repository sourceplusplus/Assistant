/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.insight.pass.multipath

import spp.jetbrains.insight.InsightKeys
import spp.jetbrains.insight.pass.ProceduralMultiPathPass
import spp.jetbrains.insight.path.ProceduralMultiPath
import spp.jetbrains.insight.path.ProceduralPath

/**
 * Saves the [ProceduralPath] set to the root artifact PSI element for later use.
 */
class SavePsiMultiPathPass : ProceduralMultiPathPass() {

    override fun postProcess(multiPath: ProceduralMultiPath): ProceduralMultiPath {
        multiPath.forEach { it.rootArtifact.putUserData(InsightKeys.PROCEDURAL_MULTI_PATH.asPsiKey(), multiPath) }
        return super.postProcess(multiPath)
    }
}
