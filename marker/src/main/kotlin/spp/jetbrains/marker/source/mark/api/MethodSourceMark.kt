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
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiNameIdentifierOwner
import spp.jetbrains.marker.service.ArtifactNamingService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import java.util.*

/**
 * Represents a [SourceMark] associated to a method artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions")
abstract class MethodSourceMark(
    override val sourceFileMarker: SourceFileMarker,
    internal var psiMethod: PsiNameIdentifierOwner,
) : SourceMark {

    override var artifactQualifiedName = ArtifactNamingService.getFullyQualifiedName(psiMethod)
    override var editor: Editor? = null
    override lateinit var sourceMarkComponent: SourceMarkComponent
    override var visiblePopup: Disposable? = null
    override val isClassMark: Boolean = false
    override val isMethodMark: Boolean = true
    override val isExpressionMark: Boolean = false

    override val moduleName: String
        get() = ProjectRootManager.getInstance(sourceFileMarker.project).fileIndex
            .getModuleForFile(psiMethod.containingFile.virtualFile)!!.name

    /**
     * Line number of the gutter mark.
     * One above the method name identifier.
     * First line for class (maybe? might want to make that for package level stats in the future)
     *
     * @return gutter mark line number
     */
    override val lineNumber: Int
        get() {
            val document = psiMethod.nameIdentifier!!.containingFile.viewProvider.document
            return document!!.getLineNumber(psiMethod.nameIdentifier!!.textRange.startOffset)
        }

    @Synchronized
    override fun apply(sourceMarkComponent: SourceMarkComponent, addToMarker: Boolean, editor: Editor?) {
        this.sourceMarkComponent = sourceMarkComponent
        super.apply(addToMarker, editor)
    }

    override fun apply(addToMarker: Boolean, editor: Editor?) {
        apply(configuration.componentProvider.getComponent(this), addToMarker, editor)
    }

    override val userData = HashMap<Any, Any>()

    fun getPsiMethod(): PsiNameIdentifierOwner {
        return psiMethod
    }

    override fun getPsiElement(): PsiNameIdentifierOwner {
        return psiMethod
    }

    fun getNameIdentifier(): PsiElement {
        return psiMethod.nameIdentifier
            ?: throw PsiInvalidElementAccessException(psiMethod, "No name identifier. Artifact: $artifactQualifiedName")
    }

    fun updatePsiMethod(psiMethod: PsiNameIdentifierOwner): Boolean {
        this.psiMethod = psiMethod
        val newArtifactQualifiedName = ArtifactNamingService.getFullyQualifiedName(psiMethod)
        if (artifactQualifiedName != newArtifactQualifiedName) {
            check(sourceFileMarker.removeSourceMark(this, autoRefresh = false, autoDispose = false))
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
