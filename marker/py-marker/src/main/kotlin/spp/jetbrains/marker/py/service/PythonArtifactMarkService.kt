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
package spp.jetbrains.marker.py.service

import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.psi.PsiElement
import com.intellij.ui.treeStructure.SimpleNode
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyStatement
import spp.jetbrains.marker.py.presentation.PythonVariableRootNode
import spp.jetbrains.marker.service.define.IArtifactMarkService
import spp.jetbrains.marker.source.mark.inlay.config.InlayMarkVirtualText
import spp.protocol.artifact.ArtifactLanguage
import spp.protocol.instrument.variable.LiveVariable
import spp.protocol.instrument.variable.LiveVariableScope

class PythonArtifactMarkService : IArtifactMarkService {

    override fun displayVirtualText(
        element: PsiElement,
        virtualText: InlayMarkVirtualText,
        sink: InlayHintsSink,
        representation: InlayPresentation
    ) {
        var statement = if (element is PyStatement) element else element
        if (virtualText.useInlinePresentation) {
            if (virtualText.showAfterLastChildWhenInline) {
                if (statement is PyCallExpression) {
                    statement = statement.parent //todo: more dynamic
                }
                sink.addInlineElement(
                    statement.lastChild.textRange.endOffset,
                    virtualText.relatesToPrecedingText,
                    representation
                )
            } else {
                sink.addInlineElement(
                    statement.textRange.startOffset,
                    virtualText.relatesToPrecedingText,
                    representation
                )
            }
        } else {
            if (statement.parent is PyFunction) {
                virtualText.spacingTillMethodText = statement.parent.prevSibling.text
                    .replace("\n", "").count { it == ' ' }
            }

            var startOffset = statement.textRange.startOffset
            if (virtualText.showBeforeAnnotationsWhenBlock) {
                if (statement.parent is PyFunction) {
                    val annotations = (statement.parent as PyFunction).decoratorList?.decorators ?: emptyArray()
                    if (annotations.isNotEmpty()) {
                        startOffset = annotations[0].textRange.startOffset
                    }
                }
            }
            sink.addBlockElement(
                startOffset,
                virtualText.relatesToPrecedingText,
                virtualText.showAbove,
                0,
                representation
            )
        }
    }

    override fun toPresentationNodes(language: ArtifactLanguage, vars: List<LiveVariable>): Array<SimpleNode> {
        return arrayOf(
            PythonVariableRootNode(
                vars.filter { it.scope == LiveVariableScope.GLOBAL_VARIABLE },
                LiveVariableScope.GLOBAL_VARIABLE
            ),
            PythonVariableRootNode(
                vars.filter { it.scope == LiveVariableScope.LOCAL_VARIABLE },
                LiveVariableScope.LOCAL_VARIABLE
            )
        )
    }
}
