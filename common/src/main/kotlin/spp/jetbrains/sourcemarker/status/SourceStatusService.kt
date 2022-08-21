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
package spp.jetbrains.sourcemarker.status

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Pair

interface SourceStatusService {
    companion object {
        val KEY = Key.create<SourceStatusService>("SPP_SOURCE_STATUS_SERVICE")

        fun getInstance(project: Project): SourceStatusService {
            return project.getUserData(KEY)!!
        }
    }

    fun getCurrentStatus(): Pair<SourceStatus, String?>
    fun update(status: SourceStatus, message: String? = null)
}
