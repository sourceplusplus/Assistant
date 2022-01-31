/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.marker.source.mark.api.component.swing

import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponent
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponentProvider
import spp.jetbrains.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration
import javax.swing.JComponent

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class SwingSourceMarkComponentProvider : SourceMarkComponentProvider {

    override val defaultConfiguration = SourceMarkComponentConfiguration()

    abstract fun makeSwingComponent(sourceMark: SourceMark): JComponent

    override fun getComponent(sourceMark: SourceMark): SourceMarkComponent {
        val component = makeSwingComponent(sourceMark)
        return object : SourceMarkComponent {
            override val configuration = defaultConfiguration.copy()

            override fun getComponent(): JComponent {
                return component
            }

            override fun dispose() {
                //do nothing
            }
        }
    }

    override fun disposeComponent(sourceMark: SourceMark) {
        //do nothing
    }
}
