package spp.jetbrains.marker.py

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants
import io.vertx.core.json.JsonObject
import spp.protocol.instrument.LiveVariable

/**
 * todo: description.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class PythonVariableSimpleNode(private val variable: LiveVariable) : SimpleNode() {

    private val scheme = DebuggerUIUtil.getColorScheme(null)

    override fun getChildren(): Array<SimpleNode> {
        if (variable.liveClazz == "<class 'dict'>") {
            val dict = JsonObject(
                (variable.value as String).replace("'", "\"")
                    .replace(": True", ": true").replace(": False", ": false")
            )
            val children = mutableListOf<SimpleNode>()
            dict.map.forEach {
                children.add(PythonVariableSimpleNode(LiveVariable("'" + it.key + "'", it.value)))
            }
            return children.toTypedArray()
        }
        return emptyArray()
    }

    override fun update(presentation: PresentationData) {
        presentation.addText(variable.name, XDebuggerUIConstants.VALUE_NAME_ATTRIBUTES)
        presentation.addText(
            " = ",
            SimpleTextAttributes.fromTextAttributes(scheme.getAttributes(DefaultLanguageHighlighterColors.IDENTIFIER))
        )

        if (variable.liveClazz?.startsWith("<class '") == true) {
            presentation.addText(
                "{" + variable.liveClazz!!.substringAfter("'").substringBefore("'") + "} ",
                SimpleTextAttributes.GRAYED_ATTRIBUTES
            )
        }

        when {
            variable.liveClazz == "<class 'int'>" || variable.value is Number -> {
                presentation.addText(
                    variable.value.toString(),
                    SimpleTextAttributes.fromTextAttributes(
                        scheme.getAttributes(DefaultLanguageHighlighterColors.NUMBER)
                    )
                )
            }
            variable.liveClazz == "<class 'str'>" -> {
                presentation.addText(
                    "\"" + variable.value + "\"",
                    SimpleTextAttributes.fromTextAttributes(
                        scheme.getAttributes(DefaultLanguageHighlighterColors.STRING)
                    )
                )
            }
            else -> {
                presentation.addText(
                    variable.value.toString(),
                    SimpleTextAttributes.fromTextAttributes(
                        scheme.getAttributes(DefaultLanguageHighlighterColors.IDENTIFIER)
                    )
                )
            }
        }
    }

    override fun getEqualityObjects(): Array<Any> = arrayOf(variable)
}
