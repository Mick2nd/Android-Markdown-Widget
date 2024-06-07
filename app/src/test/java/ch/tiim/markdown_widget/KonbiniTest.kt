package ch.tiim.markdown_widget

import cc.ekblad.konbini.ParserResult
import cc.ekblad.konbini.many
import cc.ekblad.konbini.oneOf
import cc.ekblad.konbini.parseToEnd
import cc.ekblad.konbini.parser
import cc.ekblad.konbini.regex
import cc.ekblad.konbini.string
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class KonbiniTest {

    /**
     * This is an evaluation of the Konbini parser. It solves the task of transforming a math
     * equipped md string in Joplin format to flexmark format.
     */
    @Test
    fun grammar_succeeds() {
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
        val result = grammar.parseToEnd(
            """
            This is no math. ${'$'}` Inline Math `$ Single Dollar \$ escaped possible.
            $$
            This should be math.
            $$
            Again no math.
            """.trimIndent(), false
        )
        if (result is ParserResult.Ok) {
            println(result.result)
        }
        assertTrue(result !is ParserResult.Error)
    }
}
