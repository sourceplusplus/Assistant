/*
 * Source++, the open-source live coding platform.
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
package spp.jetbrains.marker.js.service

import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.psi.PsiElement
import spp.jetbrains.marker.service.define.IArtifactTypeService
import spp.protocol.artifact.ArtifactType

/**
 * todo: description.
 *
 * @since 0.7.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JavascriptArtifactTypeService : IArtifactTypeService {

    override fun getAnnotationOwnerIfAnnotation(element: PsiElement, line: Int): PsiElement? {
        TODO("Not yet implemented")
    }

    override fun isComment(element: PsiElement): Boolean {
        TODO("Not yet implemented")
    }

    override fun getType(element: PsiElement): ArtifactType? {
        return when (element) {
            is JSClass -> ArtifactType.CLASS
            is JSFunction -> ArtifactType.METHOD
            is JSExpression -> ArtifactType.EXPRESSION

            else -> null
        }
    }
}
