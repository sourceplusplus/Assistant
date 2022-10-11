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
package spp.jetbrains.marker.jvm.service

import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.ClassUtil
import spp.jetbrains.marker.service.define.IArtifactNamingService
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.jvm.service.utils.JVMMarkerUtils
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.protocol.artifact.ArtifactLanguage
import spp.protocol.artifact.ArtifactNameUtils
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.exception.LiveStackTraceElement
import spp.protocol.artifact.exception.qualifiedClassName
import spp.protocol.instrument.LiveSourceLocation

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactNamingService : IArtifactNamingService {

    companion object {
        private val log = logger<SourceMarker>()
    }

    override fun getLiveSourceLocation(
        sourceMark: SourceMark,
        lineNumber: Int,
        serviceName: String?
    ): LiveSourceLocation? {
        val locationSource = sourceMark.artifactQualifiedName.toClass()?.identifier
        if (locationSource == null) {
            log.warn("Unable to determine location source of: $sourceMark")
            return null
        }
        return LiveSourceLocation(locationSource, lineNumber, service = serviceName)
    }

    override fun getLocation(language: Language, artifactQualifiedName: ArtifactQualifiedName): String {
        var fullyQualified = artifactQualifiedName.identifier
        if (fullyQualified.contains("#")) {
            fullyQualified = fullyQualified.substring(0, fullyQualified.indexOf("#"))
        }
        val className = ArtifactNameUtils.getClassName(fullyQualified)!!
        return if (fullyQualified.contains("(")) {
            val shortFuncName = ArtifactNameUtils.getShortFunctionSignature(
                ArtifactNameUtils.removePackageNames(fullyQualified)!!
            )
            "$className.$shortFuncName"
        } else {
            className
        }
    }

    override fun getVariableName(element: PsiElement): String? {
        return if (element is PsiDeclarationStatement) {
            val localVar = element.firstChild as? PsiLocalVariable
            localVar?.name
        } else {
            null
        }
    }

    override fun getFullyQualifiedName(element: PsiElement): ArtifactQualifiedName {
        return JVMMarkerUtils.getFullyQualifiedName(element)
    }

    override fun getQualifiedClassNames(psiFile: PsiFile): List<ArtifactQualifiedName> {
        return when (psiFile) {
            is PsiClassOwner -> psiFile.classes.map {
                getFullyQualifiedName(it)
            }.toList() + psiFile.classes.flatMap { getInnerClassesRecursively(it) }.map {
                getFullyQualifiedName(it)
            }.toList()

            else -> error("Unsupported file: $psiFile")
        }
    }

    override fun findPsiFile(language: ArtifactLanguage, project: Project, frame: LiveStackTraceElement): PsiFile? {
        val psiManager = PsiManager.getInstance(project)
        val psiClass = ClassUtil.findPsiClassByJVMName(psiManager, frame.qualifiedClassName())
        return psiClass?.containingFile
    }

    private fun getInnerClassesRecursively(psiClass: PsiClass): List<PsiClass> {
        return psiClass.innerClasses.toList() + psiClass.innerClasses.flatMap { getInnerClassesRecursively(it) }
    }
}
