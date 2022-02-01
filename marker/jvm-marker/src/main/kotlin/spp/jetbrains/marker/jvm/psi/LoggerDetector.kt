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
package spp.jetbrains.marker.jvm.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.groovy.lang.psi.impl.stringValue
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.inlay.InlayMark

/**
 * Detects the presence of log statements within methods and saves log patterns.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LoggerDetector(val vertx: Vertx) {

    companion object {
        private val log = LoggerFactory.getLogger(LoggerDetector::class.java)
        private val LOGGER_STATEMENTS = SourceKey<List<DetectedLogger>>("LOGGER_STATEMENTS")

        private val LOGGER_CLASSES = setOf(
            "org.apache.logging.log4j.spi.AbstractLogger",
            "ch.qos.logback.classic.Logger",
            "org.slf4j.Logger"
        )
        private val LOGGER_METHODS = setOf(
            "trace", "debug", "info", "warn", "error"
        )
    }

    fun addLiveLog(editor: Editor, inlayMark: InlayMark, logPattern: String, lineLocation: Int) {
        //todo: better way to handle logger detector with inlay marks
        ApplicationManager.getApplication().runReadAction {
            val methodSourceMark = findMethodSourceMark(editor, inlayMark.sourceFileMarker, lineLocation)
            if (methodSourceMark != null) {
                runBlocking {
                    getOrFindLoggerStatements(methodSourceMark)
                }
                val loggerStatements = methodSourceMark.getUserData(LOGGER_STATEMENTS)!! as MutableList<DetectedLogger>
                loggerStatements.add(DetectedLogger(logPattern, "live", lineLocation))
            } else {
                val loggerStatements = inlayMark.getUserData(LOGGER_STATEMENTS) as MutableList?
                if (loggerStatements == null) {
                    inlayMark.putUserData(
                        LOGGER_STATEMENTS,
                        mutableListOf(DetectedLogger(logPattern, "live", lineLocation))
                    )
                } else {
                    loggerStatements.add(DetectedLogger(logPattern, "live", lineLocation))
                }
            }
        }
    }

    suspend fun getOrFindLoggerStatements(sourceMark: MethodSourceMark): List<DetectedLogger> {
        val loggerStatements = sourceMark.getUserData(LOGGER_STATEMENTS)
        return if (loggerStatements != null) {
            log.trace("Found logger statements: $loggerStatements")
            loggerStatements
        } else {
            val foundLoggerStatements = getOrFindLoggerStatements(sourceMark.getPsiMethod().toUElement() as UMethod).await()
            sourceMark.putUserData(LOGGER_STATEMENTS, foundLoggerStatements)
            foundLoggerStatements
        }
    }

    fun getOrFindLoggerStatements(uMethod: UMethod): Future<List<DetectedLogger>> {
        val promise = Promise.promise<List<DetectedLogger>>()
        GlobalScope.launch(vertx.dispatcher()) {
            val loggerStatements = mutableListOf<DetectedLogger>()
            try {
                loggerStatements.addAll(determineLoggerStatements(uMethod).await())
            } catch (throwable: Throwable) {
                promise.fail(throwable)
            }
            promise.tryComplete(loggerStatements)
        }
        return promise.future()
    }

    private fun determineLoggerStatements(uMethod: UMethod): Future<List<DetectedLogger>> {
        val promise = Promise.promise<List<DetectedLogger>>()
        val loggerStatements = mutableListOf<DetectedLogger>()
        ApplicationManager.getApplication().runReadAction {
            uMethod.javaPsi.accept(object : JavaRecursiveElementVisitor() {
                override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                    val methodName = expression.methodExpression.referenceName
                    if (methodName != null && LOGGER_METHODS.contains(methodName)) {
                        val resolvedMethod = expression.resolveMethod() ?: return
                        if (LOGGER_CLASSES.contains(resolvedMethod.containingClass?.qualifiedName.orEmpty())) {
                            if (expression.argumentList.expressions.firstOrNull()?.stringValue() != null) {
                                val logTemplate = expression.argumentList.expressions.first().stringValue()!!
                                loggerStatements.add(
                                    DetectedLogger(logTemplate, methodName, getLineNumber(expression) + 1)
                                )
                                log.debug("Found log statement: $logTemplate")
                            } else {
                                log.warn("No log template argument available for expression: $expression")
                            }
                        }
                    }
                }
            })
            promise.complete(loggerStatements)
        }
        return promise.future()
    }

    private fun getLineNumber(element: PsiElement, start: Boolean = true): Int {
        val document = element.containingFile.viewProvider.document
            ?: PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)
        val index = if (start) element.startOffset else element.endOffset
        if (index > (document?.textLength ?: 0)) return 0
        return document?.getLineNumber(index) ?: 0
    }

    private fun findMethodSourceMark(editor: Editor, fileMarker: SourceFileMarker, line: Int): MethodSourceMark? {
        return fileMarker.getSourceMarks().find {
            if (it is MethodSourceMark) {
                if (it.configuration.activateOnKeyboardShortcut) {
                    //+1 on end offset so match is made even right after method end
                    val incTextRange = TextRange(
                        it.getPsiMethod().textRange.startOffset,
                        it.getPsiMethod().textRange.endOffset + 1
                    )
                    incTextRange.contains(editor.logicalPositionToOffset(LogicalPosition(line - 1, 0)))
                } else {
                    false
                }
            } else {
                false
            }
        } as MethodSourceMark?
    }

    /**
     * todo: description.
     *
     * @since 0.2.1
     * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
     */
    data class DetectedLogger(
        val logPattern: String,
        val level: String,
        val lineLocation: Int
    )
}
