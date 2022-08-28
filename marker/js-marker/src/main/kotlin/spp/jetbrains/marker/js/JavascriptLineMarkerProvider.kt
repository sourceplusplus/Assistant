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
package spp.jetbrains.marker.js

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.plugin.SourceLineMarkerProvider
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.gutter.GutterMark

/**
 * Associates JavaScript [GutterMark]s to PSI elements.
 *
 * @since 0.6.8
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JavascriptLineMarkerProvider : SourceLineMarkerProvider() {

    companion object {
        private val log = logger<JavascriptLineMarkerProvider>()

        init {
            try {
                SourceFileMarker.SUPPORTED_FILE_TYPES.add(Class.forName("com.intellij.lang.javascript.psi.JSFile") as Class<out PsiFile>)
            } catch (e: NoClassDefFoundError) {
                log.warn("Javascript support is not available")
            }
        }
    }

    override fun getLineMarkerInfo(parent: PsiElement?, element: PsiElement): LineMarkerInfo<PsiElement>? {
        val fileMarker = SourceMarker.getInstance(element.project).getSourceFileMarker(element.containingFile)
        return null //todo: this
    }

    override fun getName(): String = "JavaScript source line markers"
}