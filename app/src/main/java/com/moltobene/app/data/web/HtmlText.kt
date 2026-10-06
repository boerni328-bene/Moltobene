package com.moltobene.app.data.web

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor

/**
 * Text aus HTML: ohne Auszeichnungen, Sonderzeichen wie „&amp;“ aufgelöst. [lines] behält die Zeilen bei
 * (Absätze, Listen, Zeilenumbrüche), [inline] macht eine einzige Zeile daraus.
 */
internal object HtmlText {

    /** Eine Zeile Text, z. B. ein Titel oder eine Zutat. */
    fun inline(value: String?): String {
        if (value.isNullOrBlank()) return ""
        val text = if (value.contains('<') || value.contains('&')) Jsoup.parseBodyFragment(value).text() else value
        return text.replace(INVISIBLE, "").replace(SPACES, " ").trim()
    }

    /** Text mit Zeilen, z. B. eine Zubereitung, die als HTML mit Absätzen oder als Text mit Zeilenumbrüchen dasteht. */
    fun lines(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val text = if (value.contains('<') || value.contains('&')) blockText(Jsoup.parseBodyFragment(value).body()) else value
        return text.lines().map { it.replace(INVISIBLE, "").replace(SPACES, " ").trim() }.filter { it.isNotEmpty() }
    }

    /** Der sichtbare Text eines Elements mit seinen Zeilen. */
    fun blockText(root: Element): String {
        val out = StringBuilder()
        NodeTraversor.traverse(object : NodeVisitor {
            override fun head(node: Node, depth: Int) {
                when {
                    node is TextNode -> out.append(node.text())
                    node is Element && node.normalName() == "br" -> out.append('\n')
                    node is Element && node.normalName() in BLOCKS -> out.append('\n')
                }
            }

            override fun tail(node: Node, depth: Int) {
                if (node is Element && node.normalName() in BLOCKS) out.append('\n')
            }
        }, root)
        return out.lines().joinToString("\n") { it.replace(INVISIBLE, "").replace(SPACES, " ").trim() }
            .replace(BLANK_LINES, "\n\n")
            .trim()
    }

    private val SPACES = Regex("[\\s\\u00A0]+")

    /** Weiche Trennzeichen (&shy;) und unsichtbare Zeichen, die Seiten zum Umbrechen einfügen. */
    private val INVISIBLE = Regex("[\\u00AD\\u200B\\u200C\\u200D\\u2060\\uFEFF]")
    private val BLANK_LINES = Regex("\\n{3,}")

    private val BLOCKS = setOf(
        "p", "div", "li", "ul", "ol", "h1", "h2", "h3", "h4", "h5", "h6", "tr", "table", "section", "article",
        "header", "footer", "blockquote", "dd", "dt", "dl", "figure", "figcaption", "main", "aside", "pre", "hr",
    )
}
