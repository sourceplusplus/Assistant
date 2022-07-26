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
package spp.jetbrains.marker.source.mark.guide

import com.intellij.psi.PsiNameIdentifierOwner
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.ClassSourceMark
import spp.jetbrains.marker.source.mark.guide.config.GuideMarkConfiguration
import java.util.*

/**
 * Represents a [GuideMark] associated to a class artifact.
 *
 * @since 0.4.7
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class ClassGuideMark(
    override val sourceFileMarker: SourceFileMarker,
    override var psiClass: PsiNameIdentifierOwner
) : ClassSourceMark(sourceFileMarker, psiClass), GuideMark {

    override val id: String = UUID.randomUUID().toString()
    override val configuration: GuideMarkConfiguration = SourceMarker.configuration.guideMarkConfiguration
}
