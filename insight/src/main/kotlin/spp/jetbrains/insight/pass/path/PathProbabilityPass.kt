/*
 * Source++, the continuous feedback platform for developers.
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
package spp.jetbrains.insight.pass.path

import spp.jetbrains.insight.RuntimePath
import spp.jetbrains.insight.pass.RuntimePathPass
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.marker.model.ArtifactElement
import spp.jetbrains.marker.model.IfArtifact
import spp.protocol.insight.InsightType.CONTROL_STRUCTURE_PROBABILITY
import spp.protocol.insight.InsightType.PATH_EXECUTION_PROBABILITY
import spp.protocol.insight.InsightValue

/**
 * Calculates [PATH_EXECUTION_PROBABILITY] by propagating the [CONTROL_STRUCTURE_PROBABILITY] insight.
 * The probability of each artifact is calculated by multiplying the probability of each [IfArtifact]
 * in the path. For example, if the path contains two [IfArtifact]s with probability of 0.5, the
 * following artifacts will have a base probability of 0.25.
 */
class PathProbabilityPass : RuntimePathPass {

    private lateinit var conditionOrder: Iterator<Boolean>

    override fun analyze(path: RuntimePath) {
        conditionOrder = path.evaluations.iterator()

        path.artifacts.forEach {
            if (it is IfArtifact) {
                analyze(it, conditionOrder.next(), 1.0)
            } else {
                it.data.put(
                    SourceMarkerKeys.PATH_EXECUTION_PROBABILITY,
                    InsightValue.of(PATH_EXECUTION_PROBABILITY, 1.0)
                )
            }
        }
    }

    private fun analyze(ifArtifact: IfArtifact, condition: Boolean, probability: Double) {
        val probability = calculateProbability(ifArtifact, probability, condition)
        ifArtifact.childArtifacts.forEach {
            if (it is IfArtifact) {
                analyze(it, conditionOrder.next(), probability)
            } else {
                it.data.put(
                    SourceMarkerKeys.PATH_EXECUTION_PROBABILITY,
                    InsightValue.of(PATH_EXECUTION_PROBABILITY, probability)
                )
            }
        }
    }

    private fun calculateProbability(element: ArtifactElement, baseProbability: Double, condition: Boolean): Double {
        var selfProbability = 1.0
        if (element.getUserData(SourceMarkerKeys.CONTROL_STRUCTURE_PROBABILITY.asPsiKey()) != null) {
            selfProbability = element.getUserData(SourceMarkerKeys.CONTROL_STRUCTURE_PROBABILITY.asPsiKey())!!.value
        }

        //see if probability can be determined statically
        if (element is IfArtifact) {
            val staticProbability = element.getStaticProbability()
            if (!staticProbability.isNaN()) {
                selfProbability = staticProbability
            }
        }

        //flip self probability if condition is false
        if (!condition) {
            selfProbability = 1 - selfProbability
        }

        element.putUserData(
            SourceMarkerKeys.PATH_EXECUTION_PROBABILITY.asPsiKey(),
            InsightValue.of(PATH_EXECUTION_PROBABILITY, baseProbability)
        )

        val childProbability = baseProbability * selfProbability
        element.data.put(
            SourceMarkerKeys.PATH_EXECUTION_PROBABILITY,
            InsightValue.of(PATH_EXECUTION_PROBABILITY, childProbability)
        )

        return childProbability
    }
}
