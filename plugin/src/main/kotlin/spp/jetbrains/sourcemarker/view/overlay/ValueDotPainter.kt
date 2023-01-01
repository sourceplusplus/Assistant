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
package spp.jetbrains.sourcemarker.view.overlay

import com.intellij.ui.JBColor
import com.intellij.ui.charts.*
import spp.protocol.artifact.metrics.MetricType
import java.awt.Graphics2D
import java.awt.Point
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ValueDotPainter(
    private val data: Dataset<*>,
    private val metricType: MetricType,
) : Overlay<ChartWrapper>() {

    private val postfix: String = if (metricType.requiresConversion) "%" else ""
    private var paintTime = false

    override fun paintComponent(g: Graphics2D) {
        val mouseLoc = findHoveredCoordinate()
        if (mouseLoc != null) {
            val coords = mouseLoc as Coordinates<Number, Number>
            val xy = (chart as LineChart<*, *, *>).findMinMax() as MinMax<Number, Number>
            if (!xy.isInitialized) return

            val dotPoint = (chart as LineChart<Number, Number, *>).findLocation(xy, coords)
            val radius = 4
            g.paint = data.lineColor
            g.fillOval(dotPoint.x.roundToInt() - radius, dotPoint.y.roundToInt() - radius, radius * 2, radius * 2)
            g.paint = (chart as LineChart<*, *, *>?)!!.background
            g.drawOval(dotPoint.x.roundToInt() - radius, dotPoint.y.roundToInt() - radius, radius * 2, radius * 2)

            g.color = JBColor.foreground()
            val chartValue = coords.y.toDouble()
            val valueLabel = if (metricType.requiresConversion) {
                chartValue.toString()
            } else {
                chartValue.toInt().toString()
            } + postfix

            val bounds = g.fontMetrics.getStringBounds(valueLabel, null)
            var xCord = dotPoint.x.roundToInt()
            xCord -= bounds.width.toInt() / 2
            val var14 = dotPoint.y.roundToInt() - bounds.height.toInt()
            val var15 = bounds.height.toInt() + 30
            g.drawString(valueLabel, xCord, var14.coerceAtLeast(var15))

            if (paintTime) {
                g.paint = (chart as LineChart<*, *, *>?)!!.gridLabelColor
                val timeLabel = "%.1f$postfix"
                val bounds2 = g.fontMetrics.getStringBounds(timeLabel, null)
                xCord = dotPoint.x.roundToInt()
                g.drawString(
                    timeLabel,
                    xCord - bounds2.width.toInt() / 2,
                    (chart as LineChart<*, *, *>?)!!.height - (chart as LineChart<*, *, *>?)!!.margins.bottom + bounds2.height.toInt()
                )
            }
        }
    }

    private fun findHoveredCoordinate(): Coordinates<*, *>? {
//        Iterable var2 = (Iterable) CpuAndMemoryPanel.this.dotPainters;
//        Iterator var4 = var2.iterator();
        val var10000: Any?
        //        while (true) {
//            if (var4.hasNext()) {
//                Object var5 = var4.next();
//                ValueDotPainter itx = (ValueDotPainter) var5;
//                if (itx.getMouseLocation() == null) {
//                    continue;
//                }

//                var10000 = var5;
//                break;
//            }
//
//            var10000 = null;
//            break;
//        }
        val hoveredPainter = this
        if (hoveredPainter != null) {
            val var20 = hoveredPainter.mouseLocation
            if (var20 != null) {
                val mouseLocation: Point = var20
                val hoveredChart = hoveredPainter.chart as LineChart<*, *, *>?
                val xy = hoveredChart!!.findMinMax()
                if (xy.isInitialized) {
                    val var21 = hoveredChart.margins.left
                    val var10002 = hoveredChart.width - hoveredChart.margins.right
                    var x = mouseLocation.x
                    if (var21 <= x) {
                        if (var10002 >= x) {
                            x = mouseLocation.x - hoveredChart.margins.left
                            val rat =
                                x.toDouble() * 1.0 / (hoveredChart.width - (hoveredChart.margins.left + hoveredChart.margins.right)).toDouble()
                            val value =
                                ((xy.xMax.toLong() - xy.xMin.toLong()).toDouble() * rat).roundToLong() + xy.xMin.toLong()
                            val var10 = data.data
                            val var12 = var10.iterator()
                            while (true) {
                                if (var12.hasNext()) {
                                    val var13 = var12.next()!!
                                    val (x1) = var13 as Coordinates<*, *>
                                    if (x1.toLong() <= value) {
                                        continue
                                    }
                                    var10000 = var13
                                    break
                                }
                                var10000 = null
                                break
                            }
                            return var10000 as Coordinates<*, *>?
                        }
                    }
                }
                return null
            }
        }
        return null
    }
}
