package spp.jetbrains.sourcemarker.settings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.ProjectManager
import io.vertx.core.Promise
import spp.jetbrains.sourcemarker.PluginBundle.message
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import io.vertx.core.json.DecodeException
import io.vertx.core.json.Json
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import spp.protocol.SourceMarkerServices.Instance
import spp.protocol.general.Service
import javax.swing.JComponent

/**
 * Used to view and edit plugin configuration.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerConfigurable : Configurable {

    private val log = LoggerFactory.getLogger(SourceMarkerConfigurable::class.java)
    private var form: PluginConfigurationPanel? = null
    override fun getDisplayName(): String = message("plugin_name")

    override fun isModified(): Boolean {
        val projectSettings = PropertiesComponent.getInstance(ProjectManager.getInstance().openProjects[0])
        return !projectSettings.isValueSet("sourcemarker_plugin_config") || form!!.isModified
    }

    override fun apply() {
        val updatedConfig = form!!.pluginConfig
        val projectSettings = PropertiesComponent.getInstance(ProjectManager.getInstance().openProjects[0])
        projectSettings.setValue("sourcemarker_plugin_config", Json.encode(updatedConfig))
        form!!.applySourceMarkerConfig(updatedConfig)

        val activeProject = ProjectManager.getInstance().openProjects[0]
        DumbService.getInstance(activeProject).smartInvokeLater {
            runBlocking {
                SourceMarkerPlugin.init(activeProject)
            }
        }
    }

    override fun createComponent(): JComponent = runBlocking {
        if (form == null) {
            val projectSettings = PropertiesComponent.getInstance(ProjectManager.getInstance().openProjects[0])
            val config = if (projectSettings.isValueSet("sourcemarker_plugin_config")) {
                try {
                    Json.decodeValue(
                        projectSettings.getValue("sourcemarker_plugin_config"),
                        SourceMarkerConfig::class.java
                    )
                } catch (ex: DecodeException) {
                    log.warn("Failed to decode SourceMarker configuration", ex)
                    projectSettings.unsetValue("sourcemarker_plugin_config")
                    SourceMarkerConfig()
                }
            } else {
                SourceMarkerConfig()
            }

            val servicesPromise = Promise.promise<List<Service>>()
            Instance.liveService!!.getServices(servicesPromise)
            form = PluginConfigurationPanel(config, servicesPromise.future().await())
            form!!.applySourceMarkerConfig(config)
        }
        return@runBlocking form!!.contentPane as JComponent
    }

    override fun disposeUIResources() {
        form = null
    }
}
