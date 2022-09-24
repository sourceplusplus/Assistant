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
package spp.jetbrains.command

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import spp.jetbrains.ScopeExtensions.safeRunBlocking
import spp.jetbrains.UserData
import spp.jetbrains.plugin.LiveStatusManager
import spp.protocol.platform.developer.SelfInfo
import javax.swing.Icon

@Suppress("unused")
abstract class LiveCommand(val project: Project) {
    abstract val name: String
    open val params: List<String> = emptyList()
    open val aliases: Set<String> = emptySet()
    open var selectedIcon: Icon? = null
    open var unselectedIcon: Icon? = null

    val vertx = UserData.vertx(project)
    val skywalkingMonitorService = UserData.skywalkingMonitorService(project)
    val managementService = UserData.liveManagementService(project)!!
    val viewService = UserData.liveViewService(project)!!
    val statusManager = LiveStatusManager.getInstance(project)
    val instrumentService = UserData.liveInstrumentService(project)

    open fun getDescription(): String = ""

    open fun getDescription(context: LiveLocationContext): String = getDescription()

    open fun trigger(context: LiveCommandContext) {
        ApplicationManager.getApplication().runReadAction {
            safeRunBlocking {
                triggerSuspend(context)
            }
        }
    }

    open suspend fun triggerSuspend(context: LiveCommandContext) = Unit

    open fun isAvailable(selfInfo: SelfInfo, context: LiveLocationContext): Boolean = true
}
