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

import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.lang.javascript.psi.util.JSTreeUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfTypes
import spp.jetbrains.marker.service.define.IArtifactScopeService
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.source.SourceFileMarker

/**
 * todo: description.
 *
 * @since 0.7.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JavascriptArtifactScopeService : IArtifactScopeService {

    override fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String> {
        val vars = mutableListOf<JSVariable>()
        val position = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber)!!
        JSTreeUtil.processDeclarationsInScope(position, position, { element, _ ->
            if (element is JSVariable) {
                vars.add(element)
            }
            true
        }, null, true)
        return vars.mapNotNull { it.name }
    }

    override fun isInsideFunction(element: PsiElement): Boolean {
        return element.parentOfTypes(JSFunction::class) != null
    }
}
