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
package spp.jetbrains.marker.py.model

import com.jetbrains.python.psi.PyFunction
import spp.jetbrains.artifact.model.FunctionArtifact

class PythonFunctionArtifact(override val psiElement: PyFunction) : FunctionArtifact(psiElement) {

    override fun clone(): PythonFunctionArtifact {
        return PythonFunctionArtifact(psiElement)
    }
}
