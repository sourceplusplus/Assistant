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
package spp.jetbrains.marker

import com.intellij.lang.Language
import spp.protocol.artifact.ArtifactLanguage

abstract class AbstractSourceMarkerService<T : ISourceMarkerService> {

    private val services = mutableMapOf<String, T>()

    fun addService(service: T, language: String, vararg languages: String) {
        services[language] = service
        languages.forEach { services[it] = service }
    }

    fun addService(service: T, languages: List<String>) {
        languages.forEach { services[it] = service }
    }

    internal fun getService(language: String): T {
        return services[language] ?: throw IllegalArgumentException("No service for language $language")
    }

    internal fun getService(language: Language): T {
        return getService(language.baseLanguage?.id ?: language.id)
    }

    fun getService(language: ArtifactLanguage): T {
        return when (language) {
            ArtifactLanguage.JVM -> getService("JAVA")
            ArtifactLanguage.NODEJS -> getService("JavaScript")
            ArtifactLanguage.PYTHON -> getService("Python")
        }
    }
}
