/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.view.overlay

import com.intellij.ui.JBColor
import com.intellij.ui.charts.*
import spp.protocol.artifact.metrics.MetricType
import java.awt.Graphics2D
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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

    private val shortFormatter = DateTimeFormatter.ofPattern("h:mm a")
        .withZone(ZoneId.systemDefault())
    private val fullFormatter = DateTimeFormatter.ofPattern("h:mm:ss a")
        .withZone(ZoneId.systemDefault())
    private val postfix: String = if (metricType.requiresConversion) "%" else ""
    private var paintTime = true

    override fun paintComponent(g: Graphics2D) {
        val mouseLoc = findHoveredCoordinate()
        if (mouseLoc != null) {
            val coords = mouseLoc as Coordinates<Number, Number>
            val xy = (chart as LineChart<*, *, *>).findMinMax() as MinMax<Number, Number>
            if (!xy.isInitialized) return

            val dotPoint = (chart as LineChart<Number, Number, *>).findLocation(xy, coords)
            val radius = 4
            g.paint = data.lineColor

            val theY = dotPoint.y.roundToInt() - radius
            g.fillOval(dotPoint.x.roundToInt() - radius, theY, radius * 2, radius * 2)
            g.paint = (chart as LineChart<*, *, *>?)!!.background
            g.drawOval(dotPoint.x.roundToInt() - radius, theY, radius * 2, radius * 2)

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
                g.font = g.font.deriveFont(g.font.size + 2f)
                g.paint = (chart as LineChart<*, *, *>).gridLabelColor
                xCord = dotPoint.x.roundToInt()

                val time = Instant.ofEpochMilli(coords.x.toLong())
                val timeLabel = if (time == time.truncatedTo(ChronoUnit.MINUTES)) {
                    shortFormatter.format(time)
                } else {
                    fullFormatter.format(time)
                }

                val labelBounds = g.fontMetrics.getStringBounds(timeLabel, null)
                val yCoord = chart.height - chart.margins.bottom + labelBounds.height.toInt()
                g.drawString(timeLabel, xCord - labelBounds.width.toInt() / 2, yCoord)
            }
        }
    }

    private fun findHoveredCoordinate(): Coordinates<*, *>? {
        val mouseLocation = mouseLocation
        if (mouseLocation != null) {
            val hoveredChart = chart as LineChart<*, *, *>?
            val xy = hoveredChart!!.findMinMax()
            if (xy.isInitialized) {
                val var21 = hoveredChart.margins.left
                val var10002 = hoveredChart.width - hoveredChart.margins.right
                var x = mouseLocation.x
                if (var21 <= x) {
                    if (var10002 >= x) {
                        x = mouseLocation.x - hoveredChart.margins.left
                        val idk = hoveredChart.width - (hoveredChart.margins.left + hoveredChart.margins.right)
                        val rat = x.toDouble() * 1.0 / idk
                        val idk2 = ((xy.xMax.toLong() - xy.xMin.toLong()).toDouble() * rat).roundToLong()
                        val value = idk2 + xy.xMin.toLong()
                        val var10 = data.data
                        val var12 = var10.iterator()
                        while (true) {
                            if (var12.hasNext()) {
                                val var13 = var12.next()!!
                                val (x1) = var13 as Coordinates<*, *>
                                if (x1.toLong() <= value) {
                                    continue
                                }
                                return var13
                            }
                            return null
                        }
                    }
                }
            }
        }
        return null
    }
}
