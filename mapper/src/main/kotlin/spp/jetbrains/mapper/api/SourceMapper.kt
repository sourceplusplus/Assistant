package spp.jetbrains.mapper.api

import spp.protocol.artifact.ArtifactQualifiedName
import java.util.*

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceMapper {

    //fun getCurrentMethodQualifiedName(methodQualifiedName: String, commitId: String): String

    /**
     * todo: description.
     */
    fun getMethodQualifiedName(
        methodQualifiedName: ArtifactQualifiedName,
        targetCommitId: String,
        returnBestEffort: Boolean = false
    ): Optional<ArtifactQualifiedName>
}
