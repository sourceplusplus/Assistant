/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.insight.pass.path

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import spp.jetbrains.artifact.model.CallArtifact
import spp.jetbrains.artifact.service.getFunctions
import spp.jetbrains.artifact.service.toArtifact
import spp.jetbrains.insight.ProceduralAnalyzer
import spp.jetbrains.insight.getInsights
import spp.jetbrains.marker.js.JavascriptLanguageProvider
import spp.jetbrains.marker.jvm.JVMLanguageProvider
import spp.jetbrains.marker.py.PythonLanguageProvider
import spp.jetbrains.marker.service.*
import spp.protocol.insight.InsightType

@TestDataPath("\$CONTENT_ROOT/testData/")
class UnbalancedBranchProbabilityTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()

        JVMLanguageProvider().setup(project)
        JavascriptLanguageProvider().setup(project)
        PythonLanguageProvider().setup(project)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/"
    }

    fun testUnbalancedBranchProbability() {
        doTest("kotlin", "kt")
        doTest("java", "java")
        doTest("javascript", "js")
        doTest("python", "py")
    }

    private fun doTest(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/UnbalancedBranchProbability.$extension")
        val paths = ProceduralAnalyzer().analyze(psi.getFunctions().first().toArtifact()!!)
        assertEquals(2, paths.size)

        val truePath = paths.find { it.conditions.first().first }!!
        val trueInsights = truePath.find { it is CallArtifact }?.getInsights()!!
        assertEquals(1, trueInsights.size)
        assertEquals(InsightType.PATH_EXECUTION_PROBABILITY, trueInsights.first().type)
        assertEquals(0.25, trueInsights.first().value)

        val falsePath = paths.find { !it.conditions.first().first }!!
        val falseInsights = falsePath.find { it is CallArtifact }?.getInsights()!!
        assertEquals(1, falseInsights.size)
        assertEquals(InsightType.PATH_EXECUTION_PROBABILITY, falseInsights.first().type)
        assertEquals(0.75, falseInsights.first().value)
    }
}
