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
package spp.jetbrains.marker.jvm

import com.intellij.psi.*
import org.jetbrains.uast.*
import spp.jetbrains.marker.AbstractArtifactNamingService
import spp.jetbrains.marker.source.JVMMarkerUtils
import spp.protocol.artifact.ArtifactQualifiedName

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactNamingService : AbstractArtifactNamingService {

    override fun getVariableName(element: PsiElement): String? {
        return if (element is PsiDeclarationStatement) {
            val localVar = element.firstChild as? PsiLocalVariable
            localVar?.name
        } else {
            null
        }
    }

    override fun getFullyQualifiedName(element: PsiElement): ArtifactQualifiedName {
        return when (val uElement = element.toUElement()) {
            is UClass -> JVMMarkerUtils.getFullyQualifiedName(uElement)
            is UMethod -> JVMMarkerUtils.getFullyQualifiedName(uElement)
            is UExpression -> JVMMarkerUtils.getFullyQualifiedName(element)
            is UDeclaration -> JVMMarkerUtils.getFullyQualifiedName(element)
            is UIdentifier -> JVMMarkerUtils.getFullyQualifiedName(element)
            else -> TODO("Not yet implemented")
        }
    }

    override fun getQualifiedClassNames(psiFile: PsiFile): List<ArtifactQualifiedName> {
        return when (psiFile) {
            is PsiClassOwner -> psiFile.classes.map {
                getFullyQualifiedName(it)
            }.toList()

            else -> error("Unsupported file: $psiFile")
        }
    }
}
