package com.sourceplusplus.marker.jvm.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.compiled.ClsMethodImpl
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.sourceplusplus.marker.jvm.ArtifactSearch
import com.sourceplusplus.marker.source.SourceMarkerUtils
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.protocol.extend.SqlProducerSearch
import com.sourceplusplus.marker.jvm.psi.sqlsource.SpringDataSqlSource
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.jooq.Query
import org.slf4j.LoggerFactory
import java.util.*

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PluginSqlProducerSearch(val vertx: Vertx) : SqlProducerSearch {

    companion object {
        private val log = LoggerFactory.getLogger(PluginSqlProducerSearch::class.java)
    }

    private val detectorSet = setOf(
        SpringDataSqlSource()
    )

    val possibleRegressionSources = mutableListOf<CalledMethod>()

    override suspend fun determineSource(
        query: Query,
        searchPoint: ArtifactQualifiedName
    ): Optional<ArtifactQualifiedName> {
        val promise = Promise.promise<Optional<ArtifactQualifiedName>>()
        val searchArtifact = ArtifactSearch.findArtifact(vertx ,searchPoint)
        if (searchArtifact == null) {
            promise.fail("Could not determine search point artifact")
            return promise.future().await()
        }

        ApplicationManager.getApplication().runReadAction {
            dependencySearch(searchArtifact.toUElementOfType()!!)

            GlobalScope.launch(vertx.dispatcher()) {
                var keepSearching = true
                for (method in possibleRegressionSources) { //todo: fix dupes
                    for (detector in detectorSet) {
                        try {
                            if (detector.isSqlSource(query, method)) {
                                promise.complete(
                                    Optional.of(
                                        ArtifactQualifiedName(
                                            SourceMarkerUtils.getFullyQualifiedName(method.method),
                                            "todo",
                                            ArtifactType.METHOD
                                        )
                                    )
                                )
                                keepSearching = false
                            }
                        } catch (throwable: Throwable) {
                            promise.fail(throwable)
                        }
                        if (!keepSearching) break
                    }
                    if (!keepSearching) break
                }
                promise.tryComplete(Optional.empty())
            }
        }
        return promise.future().await()
    }

    private fun dependencySearch(method: UMethod) {
        method.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                val calledMethod = node.resolve()
                if (calledMethod != null) {
                    if (calledMethod.body == null) {
                        possibleRegressionSources.add(CalledMethod(node, calledMethod))

                        if (calledMethod !is ClsMethodImpl) {
                            //interface/abstract method; search implementations
                            val implMethods = OverridingMethodsSearch.search(calledMethod).toList()
                            implMethods.forEach {
                                dependencySearch(it.toUElementOfType()!!)
                            }
                        }
                    }
                } else {
                    log.warn("Failed to resolve: $node")
                }
                return super.visitCallExpression(node)
            }
        })
    }

    /**
     * todo: description.
     */
    interface SqlSourceDeterminer {
        fun isSqlSource(query: Query, calledMethod: CalledMethod): Boolean
    }

    /**
     * todo: description.
     */
    data class CalledMethod(
        val call: UCallExpression,
        val method: PsiMethod
    )
}
