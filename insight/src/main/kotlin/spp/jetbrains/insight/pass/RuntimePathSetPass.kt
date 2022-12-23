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
package spp.jetbrains.insight.pass

import spp.jetbrains.insight.RuntimePath
import spp.jetbrains.marker.model.analysis.IRuntimePath

/**
 * A pass that analyzes a set of [IRuntimePath]s and adds data to them.
 */
interface RuntimePathSetPass : IPass {
    fun preProcess(pathSet: Set<RuntimePath>): Set<RuntimePath> = pathSet
    fun analyze(pathSet: Set<RuntimePath>): Set<RuntimePath> = pathSet
    fun postProcess(pathSet: Set<RuntimePath>): Set<RuntimePath> = pathSet
}
