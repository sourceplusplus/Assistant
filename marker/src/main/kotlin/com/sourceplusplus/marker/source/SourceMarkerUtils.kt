package com.sourceplusplus.marker.source

import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.marker.source.mark.gutter.ClassGutterMark
import com.sourceplusplus.marker.source.mark.gutter.ExpressionGutterMark
import com.sourceplusplus.marker.source.mark.gutter.MethodGutterMark
import com.sourceplusplus.marker.source.mark.inlay.ExpressionInlayMark
import com.sourceplusplus.marker.source.mark.inlay.MethodInlayMark
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.uast.*
import org.slf4j.LoggerFactory
import java.awt.Point
import java.util.*

/**
 * Utility functions for working with [SourceMark]s.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("TooManyFunctions")
object SourceMarkerUtils {

    private val log = LoggerFactory.getLogger(SourceMarkerUtils::class.java)

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getElementAtLine(file: PsiFile, line: Int): PsiElement? {
        val document: Document = PsiDocumentManager.getInstance(file.project).getDocument(file)!!
        if (document.lineCount == line - 1) {
            return null
        }

        val offset = document.getLineStartOffset(line - 1)
        var element = file.viewProvider.findElementAt(offset)
        if (element != null) {
            if (document.getLineNumber(element.textOffset) != line - 1) {
                element = element.nextSibling
            }
        }

        if (element != null && getLineNumber(element) != line) {
            return null
        }

        return element
    }

    /**
     * todo: description.
     *
     * @since 0.3.0
     */
    @JvmStatic
    fun isBlankLine(psiFile: PsiFile, lineNumber: Int): Boolean {
        val element = getElementAtLine(psiFile, lineNumber)
        if (element != null) {
            return getLineNumber(element) != lineNumber
        }
        return true
    }

    /**
     * todo: description.
     *
     * @since 0.3.0
     */
    @JvmStatic
    fun getLineNumber(element: PsiElement): Int {
        val document = element.containingFile.viewProvider.document
        return document!!.getLineNumber(element.textRange.startOffset) + 1
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    @JvmOverloads
    fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean = false
    ): Optional<ExpressionInlayMark> {
        val element = getElementAtLine(fileMarker.psiFile, lineNumber)
        return if (element is PsiStatement) {
            Optional.ofNullable(getOrCreateExpressionInlayMark(fileMarker, element, autoApply = autoApply))
        } else if (element is PsiElement) {
            Optional.ofNullable(getOrCreateExpressionInlayMark(fileMarker, element, autoApply = autoApply))
        } else {
            Optional.empty()
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiStatement,
        autoApply: Boolean = false
    ): ExpressionInlayMark? {
        log.trace("getOrCreateExpressionInlayMark: $element")
        val statementExpression: PsiElement = getUniversalExpression(element)
        var lookupExpression: PsiElement = statementExpression
        if (lookupExpression is PsiDeclarationStatement) {
            //todo: support for multi-declaration statements
            lookupExpression = lookupExpression.firstChild
        }

        var inlayMark = lookupExpression.getUserData(SourceKey.InlayMark) as ExpressionInlayMark?
        if (inlayMark == null) {
            inlayMark = fileMarker.getExpressionSourceMark(
                lookupExpression,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark?
            if (inlayMark != null) {
                if (inlayMark.updatePsiExpression(statementExpression.toUElement() as UExpression)) {
                    statementExpression.putUserData(SourceKey.InlayMark, inlayMark)
                } else {
                    inlayMark = null
                }
            }
        }

        return if (inlayMark == null) {
            inlayMark = fileMarker.createSourceMark(
                statementExpression.toUElement() as UExpression,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark
            return if (autoApply) {
                if (inlayMark.canApply()) {
                    inlayMark.apply(true)
                    inlayMark
                } else {
                    null
                }
            } else {
                inlayMark
            }
        } else {
            if (fileMarker.removeIfInvalid(inlayMark)) {
                statementExpression.putUserData(SourceKey.InlayMark, null)
                null
            } else {
                inlayMark
            }
        }
    }

    /**
     * todo: description.
     *
     * @since 0.3.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = false
    ): ExpressionInlayMark? {
        log.trace("getOrCreateExpressionInlayMark: $element")
        var inlayMark = element.getUserData(SourceKey.InlayMark) as ExpressionInlayMark?
        if (inlayMark == null) {
            inlayMark = fileMarker.getExpressionSourceMark(
                element,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark?
            if (inlayMark != null) {
                if (inlayMark.updatePsiExpression(element.toUElement() as UExpression)) {
                    element.putUserData(SourceKey.InlayMark, inlayMark)
                } else {
                    inlayMark = null
                }
            }
        }

        return if (inlayMark == null) {
            val uExpression = element.toUElement()
            if (uExpression !is UExpression) return null
            inlayMark = fileMarker.createSourceMark(
                uExpression,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark
            return if (autoApply) {
                if (inlayMark.canApply()) {
                    inlayMark.apply(true)
                    inlayMark
                } else {
                    null
                }
            } else {
                inlayMark
            }
        } else {
            if (fileMarker.removeIfInvalid(inlayMark)) {
                element.putUserData(SourceKey.InlayMark, null)
                null
            } else {
                inlayMark
            }
        }
    }

    /**
     * todo: description.
     *
     * @since 0.3.0
     */
    @JvmStatic
    @JvmOverloads
    fun createExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean = false
    ): ExpressionInlayMark {
        val element = getElementAtLine(fileMarker.psiFile, lineNumber) as PsiStatement
        return createExpressionInlayMark(fileMarker, element, autoApply = autoApply)
    }

    /**
     * todo: description.
     *
     * @since 0.3.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun createExpressionInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiStatement,
        autoApply: Boolean = false
    ): ExpressionInlayMark {
        log.trace("createExpressionInlayMark: $element")
        val statementExpression: PsiElement = getUniversalExpression(element)
        val inlayMark = fileMarker.createSourceMark(
            statementExpression.toUElement() as UExpression,
            SourceMark.Type.INLAY
        ) as ExpressionInlayMark
        return if (autoApply) {
            if (inlayMark.canApply()) {
                inlayMark.apply(true)
                inlayMark
            } else {
                throw IllegalStateException("Could not apply inlay mark: $inlayMark")
            }
        } else {
            inlayMark
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    @JvmOverloads
    fun getOrCreateExpressionGutterMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean = false
    ): Optional<ExpressionGutterMark> {
        val element = getElementAtLine(fileMarker.psiFile, lineNumber)
        return if (element is PsiStatement) {
            Optional.ofNullable(getOrCreateExpressionGutterMark(fileMarker, element, autoApply = autoApply))
        } else Optional.empty()
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun getOrCreateExpressionGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiStatement,
        autoApply: Boolean = false
    ): ExpressionGutterMark? {
        log.trace("getOrCreateExpressionGutterMark: $element")
        val statementExpression: PsiElement = getUniversalExpression(element)
        var lookupExpression: PsiElement = statementExpression
        if (lookupExpression is PsiDeclarationStatement) {
            //todo: support for multi-declaration statements
            lookupExpression = lookupExpression.firstChild
        }

        var gutterMark = lookupExpression.getUserData(SourceKey.GutterMark) as ExpressionGutterMark?
        if (gutterMark == null) {
            gutterMark = fileMarker.getExpressionSourceMark(
                lookupExpression,
                SourceMark.Type.GUTTER
            ) as ExpressionGutterMark?
            if (gutterMark != null) {
                if (gutterMark.updatePsiExpression(statementExpression.toUElement() as UExpression)) {
                    statementExpression.putUserData(SourceKey.GutterMark, gutterMark)
                } else {
                    gutterMark = null
                }
            }
        }

        return if (gutterMark == null) {
            gutterMark = fileMarker.createSourceMark(
                statementExpression.toUElement() as UExpression,
                SourceMark.Type.GUTTER
            ) as ExpressionGutterMark
            return if (autoApply) {
                if (gutterMark.canApply()) {
                    gutterMark.apply(true)
                    gutterMark
                } else {
                    null
                }
            } else {
                gutterMark
            }
        } else {
            if (fileMarker.removeIfInvalid(gutterMark)) {
                statementExpression.putUserData(SourceKey.InlayMark, null)
                null
            } else {
                gutterMark
            }
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getUniversalExpression(element: PsiStatement): PsiElement {
        var statementExpression: PsiElement = element
        if (statementExpression is PsiExpressionStatement) {
            statementExpression = statementExpression.firstChild
        }
        return statementExpression
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun getOrCreateMethodInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = false
    ): MethodInlayMark? {
        var inlayMark = element.getUserData(SourceKey.InlayMark) as MethodInlayMark?
        if (inlayMark == null) {
            inlayMark = fileMarker.getMethodSourceMark(element.parent, SourceMark.Type.INLAY) as MethodInlayMark?
            if (inlayMark != null) {
                if (inlayMark.updatePsiMethod(element.parent.toUElement() as UMethod)) {
                    element.putUserData(SourceKey.InlayMark, inlayMark)
                } else {
                    inlayMark = null
                }
            }
        }

        return if (inlayMark == null) {
            inlayMark = fileMarker.createSourceMark(
                element.parent.toUElement() as UMethod,
                SourceMark.Type.INLAY
            ) as MethodInlayMark
            return if (autoApply) {
                if (inlayMark.canApply()) {
                    inlayMark.apply(true)
                    inlayMark
                } else {
                    null
                }
            } else {
                inlayMark
            }
        } else {
            if (fileMarker.removeIfInvalid(inlayMark)) {
                element.putUserData(SourceKey.InlayMark, null)
                null
            } else {
                inlayMark
            }
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun getOrCreateMethodGutterMark(
        fileMarker: SourceFileMarker,
        psiMethod: PsiMethod,
        autoApply: Boolean = true
    ): MethodGutterMark? {
        return getOrCreateMethodGutterMark(fileMarker, psiMethod.nameIdentifier!!, autoApply)
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun getOrCreateMethodGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = true
    ): MethodGutterMark? {
        var gutterMark = element.getUserData(SourceKey.GutterMark) as MethodGutterMark?
        if (gutterMark == null) {
            gutterMark = fileMarker.getMethodSourceMark(element.parent, SourceMark.Type.GUTTER) as MethodGutterMark?
            if (gutterMark != null) {
                if (gutterMark.updatePsiMethod(element.parent.toUElement() as UMethod)) {
                    element.putUserData(SourceKey.GutterMark, gutterMark)
                } else {
                    gutterMark = null
                }
            }
        }

        if (gutterMark == null) {
            gutterMark = fileMarker.createSourceMark(
                element.parent.toUElement() as UMethod,
                SourceMark.Type.GUTTER
            ) as MethodGutterMark
            return if (autoApply) {
                if (gutterMark.canApply()) {
                    gutterMark.apply(true)
                    gutterMark
                } else {
                    null
                }
            } else {
                gutterMark
            }
        } else {
            return when {
                fileMarker.removeIfInvalid(gutterMark) -> {
                    element.putUserData(SourceKey.GutterMark, null)
                    null
                }
                gutterMark.configuration.icon != null -> {
                    gutterMark.setVisible(true)
                    gutterMark
                }
                else -> {
                    gutterMark.setVisible(false)
                    gutterMark
                }
            }
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun getOrCreateClassGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = true
    ): ClassGutterMark? {
        var gutterMark = element.getUserData(SourceKey.GutterMark) as ClassGutterMark?
        if (gutterMark == null) {
            gutterMark = fileMarker.getClassSourceMark(element.parent, SourceMark.Type.GUTTER) as ClassGutterMark?
            if (gutterMark != null) {
                if (gutterMark.updatePsiClass(element.parent.toUElement() as UClass)) {
                    element.putUserData(SourceKey.GutterMark, gutterMark)
                } else {
                    gutterMark = null
                }
            }
        }

        if (gutterMark == null) {
            val uClass = element.parent.toUElement() as UClass
            if (uClass.qualifiedName == null) {
                log.warn("Could not determine qualified name of class: {}", uClass)
                return null
            }
            gutterMark = fileMarker.createSourceMark(
                uClass,
                SourceMark.Type.GUTTER
            ) as ClassGutterMark
            return if (autoApply) {
                if (gutterMark.canApply()) {
                    gutterMark.apply(true)
                    gutterMark
                } else {
                    null
                }
            } else {
                gutterMark
            }
        } else {
            return when {
                fileMarker.removeIfInvalid(gutterMark) -> {
                    element.putUserData(SourceKey.GutterMark, null)
                    null
                }
                gutterMark.configuration.icon != null -> {
                    gutterMark.setVisible(true)
                    gutterMark
                }
                else -> {
                    gutterMark.setVisible(false)
                    gutterMark
                }
            }
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getNameIdentifier(nameIdentifierOwner: PsiNameIdentifierOwner): PsiElement? {
        return when {
            nameIdentifierOwner.language === Language.findLanguageByID("kotlin") -> {
                when (nameIdentifierOwner) {
                    is KtNamedFunction -> nameIdentifierOwner.nameIdentifier
                    else -> (nameIdentifierOwner.navigationElement as KtNamedFunction).nameIdentifier
                }
            }
            nameIdentifierOwner.language === Language.findLanguageByID("Groovy") -> {
                (nameIdentifierOwner.navigationElement as GrMethod).nameIdentifierGroovy //todo: why can't be null?
            }
            else -> nameIdentifierOwner.nameIdentifier
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getQualifiedClassName(qualifiedName: String): String {
        var withoutArgs = qualifiedName.substring(0, qualifiedName.indexOf("("))
        return if (withoutArgs.contains("<")) {
            withoutArgs = withoutArgs.substring(0, withoutArgs.indexOf("<"))
            withoutArgs.substring(withoutArgs.lastIndexOf("?") + 1, withoutArgs.lastIndexOf("."))
        } else {
            withoutArgs.substring(withoutArgs.lastIndexOf("?") + 1, withoutArgs.lastIndexOf("."))
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getFullyQualifiedName(expression: UExpression): String {
        val qualifiedMethodName = expression.getContainingUMethod()?.let { getFullyQualifiedName(it) }
        return """$qualifiedMethodName#${Base64.getEncoder().encodeToString(expression.toString().toByteArray())}"""
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getFullyQualifiedName(method: PsiMethod): String {
        return getFullyQualifiedName(method.toUElement() as UMethod)
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getFullyQualifiedName(method: UMethod): String {
        //todo: PsiUtil.getMemberQualifiedName(method)!!
        return "${method.containingClass!!.qualifiedName}.${getQualifiedName(method)}"
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getFullyQualifiedName(theClass: UClass): String {
        //todo: PsiUtil.getMemberQualifiedName(method)!!
        return "${theClass.qualifiedName}"
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun getQualifiedName(method: UMethod): String {
        val methodName = method.nameIdentifier!!.text
        var methodParams = ""
        method.parameterList.parameters.forEach {
            if (methodParams.isNotEmpty()) {
                methodParams += ","
            }
            val qualifiedType = PsiUtil.resolveClassInType(it.type)
            val arrayDimensions = getArrayDimensions(it.type.toString())
            if (qualifiedType != null) {
                methodParams += if (qualifiedType.containingClass != null) {
                    qualifiedType.containingClass!!.qualifiedName + '$' + qualifiedType.name
                } else {
                    qualifiedType.qualifiedName
                }
                repeat(arrayDimensions) {
                    methodParams += "[]"
                }
            } else if (it.typeElement != null) {
                methodParams += it.typeElement!!.text
            } else {
                log.warn("Unable to detect element type: {}", it)
            }
        }
        return "$methodName($methodParams)"
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     */
    @JvmStatic
    fun convertPointToLineNumber(project: Project, p: Point): Int {
        val myEditor = FileEditorManager.getInstance(project).selectedTextEditor
        val document = myEditor!!.document
        val line = EditorUtil.yPositionToLogicalLine(myEditor, p)
        if (!isValidLine(document, line)) return -1
        val startOffset = document.getLineStartOffset(line)
        val region = myEditor.foldingModel.getCollapsedRegionAtOffset(startOffset)
        return if (region != null) {
            document.getLineNumber(region.endOffset)
        } else line
    }

    private fun isValidLine(document: Document, line: Int): Boolean {
        if (line < 0) return false
        val lineCount = document.lineCount
        return if (lineCount == 0) line == 0 else line < lineCount
    }

    private fun getArrayDimensions(s: String): Int {
        var arrayDimensions = 0
        for (element in s) {
            if (element == '[') {
                arrayDimensions++
            }
        }
        return arrayDimensions
    }
}
