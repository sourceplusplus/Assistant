package com.sourceplusplus.mapper.extend

import spp.protocol.artifact.ArtifactQualifiedName

/**
 * Used to tokenize source code.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceCodeTokenizer {

    fun getMethods(filename: String, sourceCode: String): List<TokenizedMethod>

    /**
     * Represents a method split by language tokens.
     */
    data class TokenizedMethod(
        val artifactQualifiedName: ArtifactQualifiedName,
        val tokens: List<String>
    )
}
