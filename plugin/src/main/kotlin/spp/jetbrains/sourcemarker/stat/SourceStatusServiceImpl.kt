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
package spp.jetbrains.sourcemarker.stat

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Pair
import com.intellij.serviceContainer.AlreadyDisposedException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import spp.jetbrains.ScopeExtensions.safeGlobalAsync
import spp.jetbrains.ScopeExtensions.safeGlobalLaunch
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import spp.jetbrains.sourcemarker.statusBar.SourceStatusBarWidget
import spp.jetbrains.status.SourceStatus
import spp.jetbrains.status.SourceStatus.*
import spp.jetbrains.status.SourceStatusService

class SourceStatusServiceImpl(val project: Project) : SourceStatusService {

    companion object {
        private val log = logger<SourceStatusServiceImpl>()
    }

    private val statusLock = Any()
    private var status: SourceStatus = Pending
    private var message: String? = null
    private val reconnectionLock = Any()
    private var reconnectionJob: Job? = null

    override fun getCurrentStatus(): Pair<SourceStatus, String?> {
        synchronized(statusLock) {
            return Pair(status, message)
        }
    }

    override fun update(status: SourceStatus, message: String?) {
        synchronized(statusLock) {
            val oldStatus = this.status
            if (oldStatus == Disabled && status != Enabled) {
                log.info("Ignoring status update from $oldStatus to $status")
                return@synchronized
            }

            if (oldStatus != status) {
                this.status = status
                this.message = message

                log.info("Status changed from $oldStatus to $status")
                safeGlobalLaunch {
                    onStatusChanged(status)
                }
            }
        }

        updateAllStatusBarIcons()
    }

    private suspend fun onStatusChanged(status: SourceStatus) = when (status) {
        ConnectionError -> {
            SourceMarkerPlugin.getInstance(project).restartIfNecessary()

            //start reconnection loop
            synchronized(reconnectionLock) {
                reconnectionJob = launchPeriodicInit(15_000, true)
            }
        }

        Enabled -> {
            SourceMarkerPlugin.getInstance(project).init()
        }

        Disabled -> {
            SourceMarkerPlugin.getInstance(project).restartIfNecessary()
            stopReconnectionLoop()
        }

        else -> {
            stopReconnectionLoop()
        }
    }

    private fun stopReconnectionLoop() {
        synchronized(reconnectionLock) {
            reconnectionJob?.cancel()
            reconnectionJob = null
        }
    }

    private fun updateAllStatusBarIcons() {
        val action = Runnable {
            for (project in ProjectManager.getInstance().openProjects) {
                if (!project.isDisposed) {
                    SourceStatusBarWidget.update(project)
                }
            }
        }
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) {
            action.run()
        } else {
            application.invokeLater(action)
        }
    }

    private fun launchPeriodicInit(
        repeatMillis: Long,
        waitBefore: Boolean
    ) = safeGlobalAsync {
        while (isActive) {
            if (waitBefore) delay(repeatMillis)
            try {
                SourceMarkerPlugin.getInstance(project).init()
            } catch (ignore: AlreadyDisposedException) {
                log.info("${project.name} is disposed, stopping reconnection loop")
                break
            }
            if (!waitBefore) delay(repeatMillis)
        }
    }
}
