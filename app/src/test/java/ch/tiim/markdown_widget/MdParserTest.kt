package ch.tiim.markdown_widget

import org.jsoup.Jsoup
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * Parameterized test.
 * Each test compares the parsed markdown with the expected html (see below for [data].
 * [Jsoup] is used to normalize the html strings to make them comparable.
 */
@RunWith(Parameterized::class)
class MdParserTest {

    companion object {
        @JvmStatic
        @get:Parameters
        val data: Collection<Any> = listOf(
            // Simple Title
            arrayOf("# Title", """<h1 id="title">Title</h1>"""),
            // Superscript
            arrayOf("A^2^", """<p>A<sup>2</sup></p>"""),
            // Subscript
            arrayOf("A~2~", """<p>A<sub>2</sub></p>"""),
            // Table
            arrayOf(
                """
                |1|2|
                |-|-|
                |1|2|                        
                """.trimIndent(),
                """
                <table class="md_table">
                <thead>
                <tr><th>1</th><th>2</th></tr>
                </thead>
                <tbody>
                <tr><td>1</td><td>2</td></tr>
                </tbody>
                </table>"""
            ),
            // TOC
            arrayOf(
                """
                [TOC]
                # Test                
                ## Table
                """.trimIndent(),
                """
                <ul class="md_toc_list">
                <li><a href="#table">Table</a></li>
                </ul>
                <h1 id="test">Test</h1>
                <h2 id="table">Table</h2>
                """
            ),
            // Quotation
            arrayOf(
                """
                > ### This Quote may be a longer paragraph
                > How can this be styled? We use a css style here
                > See the *default.css*
                > > #### A nested Quote
                > > Working?
                """.trimIndent(),
                """
                <blockquote>
                 <h3 id="this-quote-may-be-a-longer-paragraph">This Quote may be a longer paragraph</h3>
                 <p>How can this be styled? We use a css style here See the <em>default.css</em></p>
                 <blockquote>
                  <h4 id="a-nested-quote">A nested Quote</h4>
                  <p>Working?</p>
                 </blockquote>
                </blockquote>
                """
            ),
            // Admonition
            arrayOf(
                """
                !!! abstract
                    ### Here a Title
                    
                    Hello this is the abstract text.
                """.trimIndent(),
                """
                <svg xmlns="http://www.w3.org/2000/svg" class="adm-hidden">
                 <symbol id="adm-abstract">
                  <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                   <g fill="currentColor">
                    <path d="m4.8 4.7h14.4v2.4h-14.4z" />
                    <path d="m4.8 8.7h14.4v2.4h-14.4z" />
                    <path d="m4.8 12.7h14.4v2.4h-14.4z" />
                    <path d="m4.8 16.7h7.2v2.4h-7.2z" />
                   </g>
                  </svg>
                 </symbol>
                </svg>
                <div class="adm-block adm-abstract">
                 <div class="adm-heading">
                  <svg class="adm-icon">
                   <use xlink:href="#adm-abstract" />
                  </svg><span>Abstract</span>
                 </div>
                 <div class="adm-body">
                  <h3 id="here-a-title">Here a Title</h3>
                  <p>Hello this is the abstract text.</p>
                 </div>
                </div>
                """
            ),
            // Task List
            arrayOf(
                """
                - [x] task 1 
                - [ ] task 2
                """.trimIndent(),
                """
                <ul>
                 <li class="md_task_item md_item_done"><input type="checkbox" checked>task 1</li>
                 <li class="md_task_item md_item_not_done"><input type="checkbox">task 2</li>
                </ul>
                """
            ),
            // Math
            arrayOf(
                """
                ```math
                \Gamma(n) = (n-1)!\quad\forall n\in\mathbb N
                ```
                """.trimIndent(),
                """
                <div class="katex">
                 \Gamma(n) = (n-1)!\quad\forall n\in\mathbb N
                </div>
                """
            ),
            // Math - Inline
            arrayOf(
                "Inline formula: $\u0060 \\Gamma(n) = (n-1)!\\quad\\forall n\\in\\mathbb N \u0060$ inside a paragraph",
                """<p>Inline formula: <span class="katex">
                 \Gamma(n) = (n-1)!\quad\forall n\in\mathbb N
                </span> inside a paragraph</p>"""
            ),
            // Math - 2: experimental
            arrayOf(
                """
                $$ \Gamma(n) = (n-1)!\quad\forall n\in\mathbb N $$
                """.trimIndent(),
                """
                <div class="katex">
                 \Gamma(n) = (n-1)!\quad\forall n\in\mathbb N
                </div>
                """
            ),
            // Mermaid
            arrayOf(
                """
                ```mermaid
                sequenceDiagram
                participant John
                participant Alice
                Alice->>John: Hello John, how are you?
                John-->>Alice: Great
                ```
                """.trimIndent(),
                """
                <div class="mermaid">
                 sequenceDiagram participant John participant Alice Alice-&gt;&gt;John: Hello John, how are you? John--&gt;&gt;Alice: Great
                </div>
                """
            ),
            // Markdown Link
            arrayOf(
                """
                The link [Some text](https://wikipedia.de/) to Wikipedia
                """.trimIndent(),
                """
                <p>The link <a href="https://wikipedia.de/">Some text</a> to Wikipedia</p>
                """
            ),
            // Wiki Link
            arrayOf(
                """
                The link [[link]] to Wikipedia
                """.trimIndent(),
                """
                <p>The link <a href="link">link</a> to Wikipedia</p>
                """
            ),
            // Code Fence
            arrayOf(
                """
                ```xml
                <root>
                    <item name="Bob">Sample Text</item
                    <item name="Alice">Sample Text</item
                </root>
                ```
                """.trimIndent(),
                """
                <pre><code class="language-xml">&lt;root&gt;
                    &lt;item name="Bob"&gt;Sample Text&lt;/item
                    &lt;item name="Alice"&gt;Sample Text&lt;/item
                &lt;/root&gt;
                </code></pre>
                """.trimIndent()
            ),
        )
    }

    @Parameterized.Parameter(value = 0)
    lateinit var md: String
    @Parameterized.Parameter(value = 1)
    lateinit var html: String

    private lateinit var markdownParser: MarkdownParser

    @Before
    fun setup() {
        markdownParser = MarkdownParser("")
    }

    @Test
    fun parameterizedTest() {
        val parsedMd = normalize(markdownParser.parse(md))
        val expected = normalize(html)
        println("Parsed Markdown: $parsedMd")
        println("Html: $expected")
        assertEquals(expected, parsedMd)
    }

    @After
    fun teardown() {

    }

    private fun normalize(html: String) : String {
        val dom = Jsoup.parse(html)
        return dom.body().html()
    }
}
