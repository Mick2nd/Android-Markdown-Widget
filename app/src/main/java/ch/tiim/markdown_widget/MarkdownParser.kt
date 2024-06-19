package ch.tiim.markdown_widget

import android.util.Log
import cc.ekblad.konbini.ParserResult
import cc.ekblad.konbini.many
import cc.ekblad.konbini.oneOf
import cc.ekblad.konbini.parseToEnd
import cc.ekblad.konbini.parser
import cc.ekblad.konbini.regex
import cc.ekblad.konbini.string
import com.vladsch.flexmark.ext.admonition.AdmonitionExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.SubscriptExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.gitlab.GitLabExtension
import com.vladsch.flexmark.ext.superscript.SuperscriptExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.misc.Extension
import org.jetbrains.annotations.NotNull

private const val TAG = "MarkdownParser"

/**
 * Configures the md parser and provides the *parse* method.
 */
class MarkdownParser(private val theme:String) {

    val parser: Parser
    val renderer: HtmlRenderer

    /**
     * Init block.
     */
    init {
        val options = MutableDataSet()

        // uncomment to set optional extensions
        options.set(Parser.EXTENSIONS,
            listOf(
                TablesExtension.create(),
                SubscriptExtension.create(),
                SuperscriptExtension.create(),
                // StrikethroughExtension.create(),
                TocExtension.create(),
                GitLabExtension.create(),
                AdmonitionExtension.create(),
                TaskListExtension.create(),
                WikiLinkExtension.create(),
                YamlFrontMatterExtension.create(),
                ) as @NotNull Collection<Extension>)

            .set(Parser.BLOCK_QUOTE_PARSER, true)
            .set(TablesExtension.CLASS_NAME, "md_table")
            .set(TablesExtension.MIN_SEPARATOR_DASHES, 1)
            .set(TocExtension.DIV_CLASS, "md_toc_div")
            .set(TocExtension.LIST_CLASS, "md_toc_list")
            .set(TaskListExtension.TIGHT_ITEM_CLASS, "md_task_item")
            .set(TaskListExtension.ITEM_NOT_DONE_CLASS, "md_item_not_done")
            .set(TaskListExtension.ITEM_DONE_CLASS, "md_item_done")
            .set(TaskListExtension.ITEM_NOT_DONE_MARKER, "<input type=\"checkbox\" />")
            .set(TaskListExtension.ITEM_DONE_MARKER, "<input type=\"checkbox\" checked=\"checked\" />")

        // uncomment to convert soft-breaks to hard breaks
        // options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

        parser = Parser.builder(options).build()
        renderer = HtmlRenderer.builder(options).build()
    }

    /**
     * Parses a given markdown string.
     *
     * @param md the markdown string
     * @return the generated html
     */
    fun parse(md: String): String {

        val document: Node = parser.parse(preParse(md))
        val html = renderer.render(document)

        Log.d(TAG, "Rendered MD: $html")
        return html
    }

    /**
     * Responsible for changing $$ separators of math formulae into math fences block. Also transforms
     * inline $ separators.
     */
    private fun preParse(md: String): String {
        val begin = """([^$\\]|\\.)+"""
        val simple = parser { regex(begin) }
        val simpleEndingWithoutNl = parser { regex("""$begin([^$\\\n]|\\.)""") }
        val simpleEndingWithoutTick = parser { regex("""$begin([^$\\`]|\\.)""") }
        val delimiter1 = string("$$")
        val delimiter2 = string("$")
        val nl = string("\n")
        val tick = string("`")

        val math = parser {
            delimiter1()
            many(nl)
            val math = simpleEndingWithoutNl()     // simple is greedy -> consuming nl at the end
            many(nl)
            delimiter1()
            "```math\n$math\n```"
        }
        val inlineMath = parser {
            delimiter2()
            many(tick)
            val inlineMath = simpleEndingWithoutTick()
            many(tick)
            delimiter2()
            "${'$'}`$inlineMath`$"
        }
        val part = oneOf(math, inlineMath, simple)
        val grammar = parser {
            many(part).joinToString("")
        }
        val result = grammar.parseToEnd(md, false)
        if (result is ParserResult.Ok) {
            return result.result
        }
        Log.w(TAG, "Pre-parsing markdown failed, markdown is left unchanged: $result")
        return md
    }
}
