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
package spp.jetbrains.marker.js.model

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.psi.PsiReference
import spp.jetbrains.artifact.model.ArtifactElement
import spp.jetbrains.artifact.model.CallArtifact
import spp.jetbrains.artifact.model.FunctionArtifact
import spp.jetbrains.artifact.service.toArtifact

class JavascriptCallArtifact(private val psiElement: JSCallExpression) : CallArtifact(psiElement) {

    override fun resolveFunction(): FunctionArtifact? {
        return (psiElement.methodExpression as? PsiReference)?.resolve()?.toArtifact() as? FunctionArtifact
    }

    override fun getArguments(): List<ArtifactElement> {
        return psiElement.argumentList?.arguments?.mapNotNull { it.toArtifact() } ?: emptyList()
    }

    override fun clone(): JavascriptCallArtifact {
        return JavascriptCallArtifact(psiElement)
    }
}
