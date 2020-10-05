package com.sourceplusplus.sourcemarker.listeners

import com.intellij.openapi.util.IconLoader
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.mentor.SourceMentor
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.sourcemarker.SourceMarkKeys.ENDPOINT_DETECTOR
import com.sourceplusplus.sourcemarker.psi.EndpointDetector
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PluginSourceMarkEventListener(private val sourceMentor: SourceMentor) : SourceMarkEventListener {

    private val exclamationTriangle = IconLoader.getIcon(
        "/icons/exclamation-triangle.svg",
        PluginSourceMarkEventListener::class.java
    )

    companion object {
        private val log = LoggerFactory.getLogger(PluginSourceMarkEventListener::class.java)
        private val endpointDetector = EndpointDetector()
    }

    override fun handleEvent(event: SourceMarkEvent) {
        if (event.eventCode == SourceMarkEventCode.MARK_ADDED) {
            if (event.sourceMark is MethodSourceMark) {
                val methodMark = event.sourceMark as MethodSourceMark
                methodMark.putUserData(ENDPOINT_DETECTOR, endpointDetector)

                sourceMentor.getAllMethodAdvice(
                    ArtifactQualifiedName(
                        methodMark.artifactQualifiedName,
                        "", //todo: commitId
                        ArtifactType.METHOD
                    )
                ).forEach {
//                    MarkerUtils.getOrCreateMethodGutterMark()
//                    methodMark.sourceFileMarker.applySourceMark()
                }
                //todo: gather and display markings
                //todo: gather and display advice
            }
        }
    }
}
