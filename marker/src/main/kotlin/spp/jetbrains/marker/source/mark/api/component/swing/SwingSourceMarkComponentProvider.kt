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
