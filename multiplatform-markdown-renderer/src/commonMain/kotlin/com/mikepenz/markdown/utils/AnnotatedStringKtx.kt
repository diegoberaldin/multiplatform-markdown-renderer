package com.mikepenz.markdown.utils

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mikepenz.markdown.compose.LocalMarkdownColors
import com.mikepenz.markdown.compose.LocalReferenceLinkHandler
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.MarkdownTokenTypes.Companion.TEXT
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

@Composable
internal fun AnnotatedString.Builder.appendMarkdownLink(content: String, node: ASTNode) {
    val linkText = node.findChildOfType(MarkdownElementTypes.LINK_TEXT)?.children?.innerList()
    if (linkText == null) {
        append(node.getTextInNode(content).toString())
        return
    }
    val destination = node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content)?.toString()
    val linkLabel = node.findChildOfType(MarkdownElementTypes.LINK_LABEL)?.getTextInNode(content)?.toString()
    val anchorText = node.getLinkAnchorText(content)
    (anchorText ?: destination ?: linkLabel)?.also { label ->
        pushStringAnnotation(TAG_URL, label)
    }
    (anchorText ?: linkLabel)?.also { label ->
        if (destination != null) {
            LocalReferenceLinkHandler.current.store(label, destination)
        }
    }
    pushStyle(SpanStyle(color = LocalMarkdownColors.current.linkText, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold))
    buildMarkdownAnnotatedString(content, linkText)
    pop()
}

@Composable
internal fun AnnotatedString.Builder.appendAutoLink(content: String, node: ASTNode) {
    val destination = node.getTextInNode(content).toString().takeIf { it.isNotEmpty() }
    val anchorText = node.getLinkAnchorText(content)
    (anchorText ?: destination)?.also {  label ->
        pushStringAnnotation(TAG_URL, label)
        if (destination != null) {
            LocalReferenceLinkHandler.current.store(label, destination)
        }
    }
    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold))
    append(destination)
    pop()
}

/**
 * Builds an [AnnotatedString] with the contents of the given Markdown [ASTNode] node.
 *
 * This method automatically constructs the string with child components like:
 * - Paragraph
 * - Image
 * - Strong
 * - ...
 */
@Composable
fun AnnotatedString.Builder.buildMarkdownAnnotatedString(content: String, node: ASTNode) {
    buildMarkdownAnnotatedString(content, node.children)
}

/**
 * Builds an [AnnotatedString] with the contents of the given Markdown [ASTNode] node.
 *
 * This method automatically constructs the string with child components like:
 * - Paragraph
 * - Image
 * - Strong
 * - ...
 */
@Composable
fun AnnotatedString.Builder.buildMarkdownAnnotatedString(content: String, children: List<ASTNode>) {
    children.forEach { child ->
        when (child.type) {
            MarkdownElementTypes.PARAGRAPH -> buildMarkdownAnnotatedString(content, child)
            MarkdownElementTypes.IMAGE -> child.findChildOfTypeRecursive(MarkdownElementTypes.LINK_DESTINATION)?.let {
                appendInlineContent(TAG_IMAGE_URL, it.getTextInNode(content).toString())
            }

            MarkdownElementTypes.EMPH -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                buildMarkdownAnnotatedString(content, child)
                pop()
            }

            MarkdownElementTypes.STRONG -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                buildMarkdownAnnotatedString(content, child)
                pop()
            }

            MarkdownElementTypes.CODE_SPAN -> {
                pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = LocalMarkdownColors.current.inlineCodeBackground))
                append(' ')
                buildMarkdownAnnotatedString(content, child.children.innerList())
                append(' ')
                pop()
            }

            MarkdownElementTypes.AUTOLINK -> appendAutoLink(content, child)
            MarkdownElementTypes.INLINE_LINK -> appendMarkdownLink(content, child)
            MarkdownElementTypes.SHORT_REFERENCE_LINK -> appendMarkdownLink(content, child)
            MarkdownElementTypes.FULL_REFERENCE_LINK -> appendMarkdownLink(content, child)
            TEXT -> append(child.getTextInNode(content).toString())
            GFMTokenTypes.GFM_AUTOLINK -> if (child.parent == MarkdownElementTypes.LINK_TEXT) {
                append(child.getTextInNode(content).toString())
            } else appendAutoLink(content, child)

            MarkdownTokenTypes.SINGLE_QUOTE -> append('\'')
            MarkdownTokenTypes.DOUBLE_QUOTE -> append('\"')
            MarkdownTokenTypes.LPAREN -> append('(')
            MarkdownTokenTypes.RPAREN -> append(')')
            MarkdownTokenTypes.LBRACKET -> append('[')
            MarkdownTokenTypes.RBRACKET -> append(']')
            MarkdownTokenTypes.LT -> append('<')
            MarkdownTokenTypes.GT -> append('>')
            MarkdownTokenTypes.COLON -> append(':')
            MarkdownTokenTypes.EXCLAMATION_MARK -> append('!')
            MarkdownTokenTypes.BACKTICK -> append('`')
            MarkdownTokenTypes.HARD_LINE_BREAK -> append("\n\n")
            MarkdownTokenTypes.EOL -> append('\n')
            MarkdownTokenTypes.WHITE_SPACE -> if (length > 0) {
                append(' ')
            }
        }
    }
}
