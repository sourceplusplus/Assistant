/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.marker.source

import com.google.common.collect.ImmutableList
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.source.mark.api.*
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.gutter.ClassGutterMark
import spp.jetbrains.marker.source.mark.gutter.ExpressionGutterMark
import spp.jetbrains.marker.source.mark.gutter.MethodGutterMark
import spp.jetbrains.marker.source.mark.inlay.ExpressionInlayMark
import spp.jetbrains.marker.source.mark.inlay.MethodInlayMark
import spp.protocol.artifact.ArtifactQualifiedName
import java.util.*

/**
 * Used to mark a source code file with SourceMarker artifact marks.
 * SourceMarker artifact marks can be used to subscribe to and collect source code runtime information.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions")
open class SourceFileMarker(val psiFile: PsiFile) : SourceMarkProvider {

    companion object {
        val KEY = Key.create<SourceFileMarker>("sm.SourceFileMarker")
        private val log = LoggerFactory.getLogger(SourceFileMarker::class.java)
        val SUPPORTED_FILE_TYPES = mutableListOf<Class<out PsiFile>>()

        @JvmStatic
        fun isFileSupported(psiFile: PsiFile): Boolean {
            return SUPPORTED_FILE_TYPES.any { it.isInstance(psiFile) }
        }
    }

    private val sourceMarks: MutableSet<SourceMark> = Collections.synchronizedSet(
        Collections.newSetFromMap(IdentityHashMap())
    )
    val project: Project = psiFile.project

    /**
     * Gets the [SourceMark]s recognized in the current source code file.
     *
     * @return a list of the [SourceMark]s
     */
    open fun getSourceMarks(): List<SourceMark> {
        return ImmutableList.copyOf(sourceMarks)
    }

    open fun refresh() {
        if (!psiFile.project.isDisposed && !ApplicationManager.getApplication().isUnitTestMode) {
            try {
                DaemonCodeAnalyzer.getInstance(psiFile.project).restart(psiFile)
            } catch (ignored: ProcessCanceledException) {
            }
        }
    }

    open fun clearSourceMarks() {
        val removed = sourceMarks.removeIf {
            it.dispose(false)
            true
        }
        if (removed) refresh()
    }

    open suspend fun clearSourceMarksSuspend() {
        val removed = sourceMarks.removeIf {
            runBlocking {
                it.disposeSuspend(false)
            }
            true
        }
        if (removed) refresh()
    }

    open fun removeIfInvalid(sourceMark: SourceMark): Boolean {
        var removedMark = false
        if (!sourceMark.valid) {
            check(removeSourceMark(sourceMark))
            removedMark = true
        }
        if (removedMark) refresh()
        return removedMark
    }

    open fun removeInvalidSourceMarks(): Boolean {
        var removedMark = false
        sourceMarks.toList().forEach {
            if (!it.valid) {
                check(removeSourceMark(it))
                removedMark = true
            }
        }
        if (removedMark) refresh()
        return removedMark
    }

    @JvmOverloads
    open fun removeSourceMark(
        sourceMark: SourceMark,
        autoRefresh: Boolean = false,
        autoDispose: Boolean = true
    ): Boolean {
        log.trace("Removing source mark for artifact: $sourceMark")
        return if (sourceMarks.remove(sourceMark)) {
            if (autoDispose) sourceMark.dispose(false)
            if (autoRefresh) refresh()
            log.trace("Removed source mark for artifact: $sourceMark")
            true
        } else false
    }

    @JvmOverloads
    open fun applySourceMark(
        sourceMark: SourceMark,
        autoRefresh: Boolean = false,
        overrideFilter: Boolean = false
    ): Boolean {
        if (overrideFilter || sourceMark.canApply()) {
            log.trace("Applying source mark for artifact: $sourceMark")
            sourceMark.triggerEvent(SourceMarkEvent(sourceMark, SourceMarkEventCode.MARK_BEFORE_ADDED))
            if (sourceMarks.add(sourceMark)) {
                when (sourceMark) {
                    is ClassGutterMark -> sourceMark.getPsiElement().nameIdentifier!!.putUserData(
                        SourceKey.GutterMark,
                        sourceMark
                    )
                    is MethodGutterMark -> sourceMark.getPsiElement().nameIdentifier!!.putUserData(
                        SourceKey.GutterMark,
                        sourceMark
                    )
                    is MethodInlayMark -> sourceMark.getPsiElement().nameIdentifier!!.putUserData(
                        SourceKey.InlayMark,
                        sourceMark
                    )
                    is ExpressionGutterMark -> sourceMark.getPsiElement().putUserData(SourceKey.GutterMark, sourceMark)
                    is ExpressionInlayMark -> sourceMark.getPsiElement().putUserData(SourceKey.InlayMark, sourceMark)
                }

                if (autoRefresh) refresh()
                log.trace("Applied source mark for artifact: $sourceMark")
                return true
            }
        }
        return false
    }

    fun containsSourceMark(sourceMark: SourceMark): Boolean {
        return sourceMarks.contains(sourceMark)
    }

    fun containsPsiElement(psiElement: PsiElement): Boolean {
        return sourceMarks.find { it.getPsiElement() === psiElement } != null
    }

    open fun getSourceMark(artifactQualifiedName: ArtifactQualifiedName, type: SourceMark.Type): SourceMark? {
        return sourceMarks.find { it.artifactQualifiedName == artifactQualifiedName && it.type == type }
    }

    open fun getSourceMarks(artifactQualifiedName: ArtifactQualifiedName): List<SourceMark> {
        return sourceMarks.filter { it.artifactQualifiedName == artifactQualifiedName }
    }

    open fun getClassSourceMark(psiClass: PsiElement, type: SourceMark.Type): ClassSourceMark? {
        return sourceMarks.find {
            it is ClassSourceMark && it.valid && it.psiClass === psiClass && it.type == type
        } as ClassSourceMark?
    }

    open fun getMethodSourceMark(psiMethod: PsiElement, type: SourceMark.Type): MethodSourceMark? {
        return sourceMarks.find {
            it is MethodSourceMark && it.valid && it.psiMethod === psiMethod && it.type == type
        } as MethodSourceMark?
    }

    open fun getExpressionSourceMark(psiElement: PsiElement, type: SourceMark.Type): ExpressionSourceMark? {
        return sourceMarks.find {
            it is ExpressionSourceMark && it.valid && it.psiExpression === psiElement && it.type == type
        } as ExpressionSourceMark?
    }

    open fun getMethodSourceMarks(): List<MethodSourceMark> {
        return sourceMarks.filterIsInstance<MethodSourceMark>()
    }

    open fun getClassSourceMarks(): List<ClassSourceMark> {
        return sourceMarks.filterIsInstance<ClassSourceMark>()
    }

    open fun getMethodExpressionSourceMark(methodSourceMark: MethodSourceMark): List<ExpressionSourceMark> {
        return sourceMarks.filterIsInstance<ExpressionSourceMark>().filter {
            it.valid && it.psiExpression == methodSourceMark.psiMethod
        }
    }

    override fun createExpressionSourceMark(psiExpression: PsiElement, type: SourceMark.Type): ExpressionSourceMark {
        log.trace("Creating source mark. Expression: $psiExpression - Type: $type")
        return when (type) {
            SourceMark.Type.GUTTER -> {
                ExpressionGutterMark(this, psiExpression)
            }
            SourceMark.Type.INLAY -> {
                ExpressionInlayMark(this, psiExpression)
            }
        }
    }

    override fun createMethodSourceMark(
        psiMethod: PsiNameIdentifierOwner, qualifiedName: ArtifactQualifiedName, type: SourceMark.Type
    ): MethodSourceMark {
        log.trace("Creating source mark. Method: $qualifiedName - Type: $type")
        return when (type) {
            SourceMark.Type.GUTTER -> {
                MethodGutterMark(this, psiMethod)
            }
            SourceMark.Type.INLAY -> {
                MethodInlayMark(this, psiMethod)
            }
        }
    }

    override fun createClassSourceMark(
        psiClass: PsiNameIdentifierOwner, qualifiedName: ArtifactQualifiedName, type: SourceMark.Type
    ): ClassSourceMark {
        log.trace("Creating source mark. Class: $qualifiedName - Type: $type")
        return when (type) {
            SourceMark.Type.GUTTER -> {
                ClassGutterMark(this, psiClass)
            }
            SourceMark.Type.INLAY -> {
                TODO("Not yet implemented")
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SourceFileMarker
        if (psiFile != other.psiFile) return false
        return true
    }

    override fun hashCode(): Int {
        return psiFile.hashCode()
    }

    override fun toString(): String {
        return "SourceFileMarker:${psiFile.name}"
    }
}
