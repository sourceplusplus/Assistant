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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import spp.jetbrains.marker.IArtifactCreationService
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.impl.ArtifactNamingService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.gutter.ExpressionGutterMark
import spp.jetbrains.marker.source.mark.gutter.MethodGutterMark
import spp.jetbrains.marker.source.mark.inlay.ExpressionInlayMark
import spp.jetbrains.marker.source.mark.inlay.MethodInlayMark
import java.util.*

/**
 * todo: description.
 *
 * @since 0.7.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JavascriptArtifactCreationService : IArtifactCreationService {

    override fun getOrCreateExpressionGutterMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): Optional<ExpressionGutterMark> {
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber)
        if (element != null) {
            return Optional.ofNullable(getOrCreateExpressionGutterMark(fileMarker, element, autoApply))
        }
        return Optional.empty()
    }

    override fun getOrCreateMethodGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): MethodGutterMark? {
        TODO()
    }

    fun getOrCreateExpressionGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = false
    ): ExpressionGutterMark? {
        var gutterMark = element.getUserData(SourceKey.GutterMark) as ExpressionGutterMark?
        if (gutterMark == null) {
            gutterMark = fileMarker.getExpressionSourceMark(
                element,
                SourceMark.Type.GUTTER
            ) as ExpressionGutterMark?
            if (gutterMark != null) {
                if (gutterMark.updatePsiExpression(element, ArtifactNamingService.getFullyQualifiedName(element))) {
                    element.putUserData(SourceKey.GutterMark, gutterMark)
                } else {
                    gutterMark = null
                }
            }
        }

        return if (gutterMark == null) {
            gutterMark = fileMarker.createExpressionSourceMark(
                element,
                SourceMark.Type.GUTTER
            ) as ExpressionGutterMark
            return if (autoApply) {
                if (gutterMark.canApply()) {
                    gutterMark.apply(true)
                    gutterMark
                } else {
                    null
                }
            } else {
                gutterMark
            }
        } else {
            if (fileMarker.removeIfInvalid(gutterMark)) {
                element.putUserData(SourceKey.InlayMark, null)
                null
            } else {
                gutterMark
            }
        }
    }

    override fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): Optional<ExpressionInlayMark> {
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber)
        if (element != null) {
            return Optional.ofNullable(getOrCreateExpressionInlayMark(fileMarker, element, autoApply))
        }
        return Optional.empty()
    }

    override fun createMethodGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): MethodGutterMark {
        return fileMarker.createMethodGutterMark(element.parent as PsiNameIdentifierOwner, autoApply)
    }

    override fun createMethodInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): MethodInlayMark {
        return fileMarker.createMethodInlayMark(element.parent as PsiNameIdentifierOwner, autoApply)
    }

    override fun createExpressionGutterMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): ExpressionGutterMark {
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber) as PsiElement
        return fileMarker.createExpressionGutterMark(element, autoApply)
    }

    override fun createExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): ExpressionInlayMark {
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber)!!
        val inlayMark = fileMarker.createExpressionSourceMark(
            element,
            SourceMark.Type.INLAY
        ) as ExpressionInlayMark
        return if (autoApply) {
            if (inlayMark.canApply()) {
                inlayMark.apply(true)
                inlayMark
            } else {
                error("Could not apply inlay mark: $inlayMark")
            }
        } else {
            inlayMark
        }
    }

    fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = false
    ): ExpressionInlayMark? {
        var inlayMark = element.getUserData(SourceKey.InlayMark) as ExpressionInlayMark?
        if (inlayMark == null) {
            inlayMark = fileMarker.getExpressionSourceMark(
                element,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark?
            if (inlayMark != null) {
                if (inlayMark.updatePsiExpression(element, ArtifactNamingService.getFullyQualifiedName(element))) {
                    element.putUserData(SourceKey.InlayMark, inlayMark)
                } else {
                    inlayMark = null
                }
            }
        }

        return if (inlayMark == null) {
            inlayMark = fileMarker.createExpressionSourceMark(
                element,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark
            return if (autoApply) {
                if (inlayMark.canApply()) {
                    inlayMark.apply(true)
                    inlayMark
                } else {
                    null
                }
            } else {
                inlayMark
            }
        } else {
            if (fileMarker.removeIfInvalid(inlayMark)) {
                element.putUserData(SourceKey.InlayMark, null)
                null
            } else {
                inlayMark
            }
        }
    }
}
