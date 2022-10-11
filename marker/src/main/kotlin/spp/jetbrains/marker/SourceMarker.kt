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

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import spp.jetbrains.ScopeExtensions.safeGlobalLaunch
import spp.jetbrains.marker.service.ArtifactNamingService
import spp.jetbrains.marker.service.SourceGuideProvider
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.gutter.GutterMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.protocol.artifact.ArtifactNameUtils
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions")
class SourceMarker {

    companion object {
        var PLUGIN_NAME = "SourceMarker"

        private val log = logger<SourceMarker>()
        private val KEY = Key.create<SourceMarker>("SPP_SOURCE_MARKER")

        @JvmStatic
        @Synchronized
        fun getInstance(project: Project): SourceMarker {
            if (project.getUserData(KEY) == null) {
                val sourceMarker = SourceMarker()
                project.putUserData(KEY, sourceMarker)
            }
            return project.getUserData(KEY)!!
        }

        fun getSourceFileMarker(psiFile: PsiFile): SourceFileMarker? {
            return getInstance(psiFile.project).getSourceFileMarker(psiFile)
        }
    }

    @Volatile
    var enabled = true
    val configuration: SourceMarkerConfiguration = SourceMarkerConfiguration()
    private val availableSourceFileMarkers = Maps.newConcurrentMap<Int, SourceFileMarker>()
    private val globalSourceMarkEventListeners = Lists.newArrayList<SourceMarkEventListener>()

    suspend fun clearAvailableSourceFileMarkers() {
        check(enabled) { "SourceMarker disabled" }

        availableSourceFileMarkers.forEach {
            deactivateSourceFileMarker(it.value)
        }
        availableSourceFileMarkers.clear()
    }

    suspend fun deactivateSourceFileMarker(sourceFileMarker: SourceFileMarker): Boolean {
        check(enabled) { "SourceMarker disabled" }

        if (availableSourceFileMarkers.remove(sourceFileMarker.hashCode()) != null) {
            sourceFileMarker.clearSourceMarks()
            sourceFileMarker.psiFile.putUserData(SourceFileMarker.KEY, null)
            log.info("Deactivated source file marker: $sourceFileMarker")
            return true
        }
        return false
    }

    fun getSourceFileMarker(psiFile: PsiFile): SourceFileMarker? {
        check(enabled) { "SourceMarker disabled" }

        var fileMarker = psiFile.getUserData(SourceFileMarker.KEY)
        if (fileMarker != null) {
            return fileMarker
        } else if (!SourceFileMarker.isFileSupported(psiFile)) {
            return null
        }

        fileMarker = configuration.sourceFileMarkerProvider.createSourceFileMarker(psiFile)
        availableSourceFileMarkers.putIfAbsent(psiFile.hashCode(), fileMarker)
        fileMarker = availableSourceFileMarkers[psiFile.hashCode()]!!
        psiFile.putUserData(SourceFileMarker.KEY, fileMarker)

        safeGlobalLaunch {
            SourceGuideProvider.determineGuideMarks(fileMarker)
        }
        return fileMarker
    }

    fun getSourceFileMarker(qualifiedClassNameOrFilename: String): SourceFileMarker? {
        check(enabled) { "SourceMarker disabled" }

        return availableSourceFileMarkers.values.find {
            ArtifactNamingService.getQualifiedClassNames(it.psiFile).find {
                it.identifier.contains(qualifiedClassNameOrFilename)
            } != null || it.psiFile.virtualFile.path.endsWith(qualifiedClassNameOrFilename)
        }
    }

    fun getSourceFileMarker(artifactQualifiedName: ArtifactQualifiedName): SourceFileMarker? {
        check(enabled) { "SourceMarker disabled" }

        val classArtifactQualifiedName = artifactQualifiedName.copy(
            identifier = ArtifactNameUtils.getQualifiedClassName(artifactQualifiedName.identifier)!!,
            type = ArtifactType.CLASS
        )
        return availableSourceFileMarkers.values.find {
            ArtifactNamingService.getQualifiedClassNames(it.psiFile).contains(classArtifactQualifiedName)
        }
    }

    fun getAvailableSourceFileMarkers(): List<SourceFileMarker> {
        check(enabled) { "SourceMarker disabled" }

        return ImmutableList.copyOf(availableSourceFileMarkers.values)
    }

    fun addGlobalSourceMarkEventListener(sourceMarkEventListener: SourceMarkEventListener) {
        log.info("Adding global source mark event listener: $sourceMarkEventListener")
        globalSourceMarkEventListeners.add(sourceMarkEventListener)
    }

    fun removeGlobalSourceMarkEventListener(sourceMarkEventListener: SourceMarkEventListener) {
        log.info("Removing global source mark event listener: $sourceMarkEventListener")
        globalSourceMarkEventListeners.remove(sourceMarkEventListener)
    }

    fun getGlobalSourceMarkEventListeners(): List<SourceMarkEventListener> {
        return ImmutableList.copyOf(globalSourceMarkEventListeners)
    }

    fun clearGlobalSourceMarkEventListeners() {
        globalSourceMarkEventListeners.clear()
    }

    fun getSourceMark(artifactQualifiedName: ArtifactQualifiedName, type: SourceMark.Type): SourceMark? {
        check(enabled) { "SourceMarker disabled" }

        availableSourceFileMarkers.values.forEach {
            val sourceMark = it.getSourceMark(artifactQualifiedName, type)
            if (sourceMark != null) {
                return sourceMark
            }
        }
        return null
    }

    fun getGuideMark(artifactQualifiedName: ArtifactQualifiedName): GuideMark? {
        return getSourceMark(artifactQualifiedName, SourceMark.Type.GUIDE) as GuideMark?
    }

    fun getSourceMarks(artifactQualifiedName: ArtifactQualifiedName): List<SourceMark> {
        check(enabled) { "SourceMarker disabled" }

        availableSourceFileMarkers.values.forEach {
            val sourceMarks = it.getSourceMarks(artifactQualifiedName)
            if (sourceMarks.isNotEmpty()) {
                return sourceMarks
            }
        }
        return emptyList()
    }

    fun getSourceMarks(): List<SourceMark> {
        check(enabled) { "SourceMarker disabled" }
        return availableSourceFileMarkers.values.flatMap { it.getSourceMarks() }
    }

    fun getSourceMark(id: String): SourceMark? {
        return getSourceMarks().find { it.id == id }
    }

    fun getInlayMarks(): List<InlayMark> {
        return getSourceMarks().filterIsInstance<InlayMark>()
    }

    fun getGutterMarks(): List<GutterMark> {
        return getSourceMarks().filterIsInstance<GutterMark>()
    }

    fun getGuideMarks(): List<GuideMark> {
        return getSourceMarks().filterIsInstance<GuideMark>()
    }
}
