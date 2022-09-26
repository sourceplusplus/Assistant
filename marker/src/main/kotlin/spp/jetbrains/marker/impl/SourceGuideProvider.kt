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
package spp.jetbrains.marker.impl

import com.intellij.openapi.application.ReadAction
import spp.jetbrains.marker.AbstractSourceGuideProvider
import spp.jetbrains.marker.source.SourceFileMarker

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceGuideProvider : AbstractSourceGuideProvider {

    private val providers = mutableMapOf<String, MutableList<AbstractSourceGuideProvider>>()

    fun addProvider(guideProvider: AbstractSourceGuideProvider, language: String, vararg languages: String) {
        providers.computeIfAbsent(language) { mutableListOf() }.add(guideProvider)
        languages.forEach { providers.computeIfAbsent(it) { mutableListOf() }.add(guideProvider) }
    }

    fun addProvider(guideProvider: AbstractSourceGuideProvider, languages: List<String>) {
        languages.forEach { providers.computeIfAbsent(it) { mutableListOf() }.add(guideProvider) }
    }

    private fun getProvider(language: String): AbstractSourceGuideProvider {
        return providers[language]?.let {
            object : AbstractSourceGuideProvider {
                override fun determineGuideMarks(fileMarker: SourceFileMarker) {
                    it.forEach { provider -> provider.determineGuideMarks(fileMarker) }
                }
            }
        } ?: throw IllegalArgumentException("No provider for language $language")
    }

    override fun determineGuideMarks(fileMarker: SourceFileMarker) {
        ReadAction.run<Nothing> {
            getProvider(fileMarker.psiFile.language.id).determineGuideMarks(fileMarker)
        }
    }
}
