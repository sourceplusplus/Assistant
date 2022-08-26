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
package spp.jetbrains.sourcemarker.service.instrument.breakpoint.tree

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.util.containers.hash.LinkedHashMap
import spp.jetbrains.marker.jvm.JVMVariableSimpleNode
import spp.jetbrains.marker.py.PythonVariableRootNode
import spp.jetbrains.sourcemarker.activities.PluginSourceMarkerStartupActivity.Companion.PYCHARM_PRODUCT_CODES
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.StackFrameManager
import spp.protocol.instrument.variable.LiveVariableScope

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class VariableRootSimpleNode : SimpleNode() {

    private lateinit var stackFrameManager: StackFrameManager

    fun setStackFrameManager(currentStackFrameManager: StackFrameManager) {
        stackFrameManager = currentStackFrameManager
    }

    override fun getChildren(): Array<SimpleNode> {
        if (!this::stackFrameManager.isInitialized) {
            return emptyArray() //wait till initialized
        }

        return if (stackFrameManager.currentFrame?.variables.isNullOrEmpty()) {
            NO_CHILDREN
        } else {
            val vars = stackFrameManager.currentFrame!!.variables
            val productCode = ApplicationInfo.getInstance().build.productCode
            if (PYCHARM_PRODUCT_CODES.contains(productCode)) {
                return arrayOf(
                    PythonVariableRootNode(
                        vars.filter { it.scope == LiveVariableScope.GLOBAL_VARIABLE },
                        LiveVariableScope.GLOBAL_VARIABLE
                    ),
                    PythonVariableRootNode(
                        vars.filter { it.scope == LiveVariableScope.LOCAL_VARIABLE },
                        LiveVariableScope.LOCAL_VARIABLE
                    )
                )
            } else {
                val simpleNodeMap: MutableMap<String, JVMVariableSimpleNode> = LinkedHashMap()
                vars.forEach {
                    if (it.name.isNotEmpty()) {
                        simpleNodeMap[it.name] = JVMVariableSimpleNode(it, mutableMapOf())
                    }
                }
                simpleNodeMap.values.sortedWith { p0, p1 ->
                    when {
                        p0.variable.name == "this" -> -1
                        p1.variable.name == "this" -> 1
                        else -> 0
                    }
                }.toTypedArray()
            }
        }
    }
}
