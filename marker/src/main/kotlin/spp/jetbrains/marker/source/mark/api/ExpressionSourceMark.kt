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
package spp.jetbrains.marker.source.mark.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import spp.jetbrains.marker.impl.ArtifactNamingService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.protocol.artifact.ArtifactQualifiedName
import java.util.*

/**
 * Represents a [SourceMark] associated to an expression artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions")
abstract class ExpressionSourceMark(
    override val sourceFileMarker: SourceFileMarker,
    internal var psiExpression: PsiElement,
) : SourceMark {

    override var artifactQualifiedName = ArtifactNamingService.getFullyQualifiedName(psiExpression)
    override var editor: Editor? = null
    override lateinit var sourceMarkComponent: SourceMarkComponent
    override var visiblePopup: Disposable? = null
    override val isClassMark: Boolean = false
    override val isMethodMark: Boolean = false
    override val isExpressionMark: Boolean = true
    override val valid: Boolean; get() {
        return try {
            psiExpression.isValid && artifactQualifiedName == ArtifactNamingService.getFullyQualifiedName(psiExpression)
        } catch (ignore: PsiInvalidElementAccessException) {
            false
        }
    }

    override val moduleName: String
        get() = TODO("moduleName")

    override val lineNumber: Int
        get() {
            val document = getPsiElement().containingFile.viewProvider.document
            return document!!.getLineNumber(getPsiElement().textRange.startOffset) + 1
        }

    override val viewProviderBound: Boolean
        get() = try {
            psiExpression.containingFile.viewProvider.document
            true
        } catch (ignore: PsiInvalidElementAccessException) {
            false
        }

    @Synchronized
    override fun apply(sourceMarkComponent: SourceMarkComponent, addToMarker: Boolean, editor: Editor?) {
        this.sourceMarkComponent = sourceMarkComponent
        super.apply(addToMarker, editor)
    }

    override fun apply(addToMarker: Boolean, editor: Editor?) {
        apply(configuration.componentProvider.getComponent(this), addToMarker, editor)
    }

    override fun dispose(removeFromMarker: Boolean, assertRemoval: Boolean) {
        when (this) {
            is GutterMark -> getPsiElement().putUserData(SourceKey.GutterMark, null)
            is InlayMark -> getPsiElement().putUserData(SourceKey.InlayMark, null)
            is GuideMark -> getPsiElement().putUserData(SourceKey.GuideMark, null)
            else -> error("Unsupported source mark type: $this")
        }
        super.dispose(removeFromMarker, assertRemoval)
    }

    override suspend fun disposeSuspend(removeFromMarker: Boolean, assertRemoval: Boolean) {
        when (this) {
            is GutterMark -> getPsiElement().putUserData(SourceKey.GutterMark, null)
            is InlayMark -> getPsiElement().putUserData(SourceKey.InlayMark, null)
            is GuideMark -> getPsiElement().putUserData(SourceKey.GuideMark, null)
            else -> error("Unsupported source mark type: $this")
        }
        super.disposeSuspend(removeFromMarker, assertRemoval)
    }

    override val userData = HashMap<Any, Any>()

    fun getPsiExpression(): PsiElement {
        return psiExpression
    }

    override fun getPsiElement(): PsiElement {
        return psiExpression
    }

    fun updatePsiExpression(psiExpression: PsiElement, newArtifactQualifiedName: ArtifactQualifiedName): Boolean {
        this.psiExpression = psiExpression
        if (artifactQualifiedName != newArtifactQualifiedName) {
            check(sourceFileMarker.removeSourceMark(this, autoRefresh = false))
            val oldArtifactQualifiedName = artifactQualifiedName
            artifactQualifiedName = newArtifactQualifiedName
            return if (sourceFileMarker.applySourceMark(this, autoRefresh = false)) {
                triggerEvent(SourceMarkEvent(this, SourceMarkEventCode.NAME_CHANGED, oldArtifactQualifiedName))
                true
            } else false
        }
        return true
    }

    override val eventListeners = ArrayList<SourceMarkEventListener>()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (hashCode() != other.hashCode()) return false
        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(artifactQualifiedName, type)
    }

    override fun toString(): String = "${javaClass.simpleName}: $artifactQualifiedName"
}
