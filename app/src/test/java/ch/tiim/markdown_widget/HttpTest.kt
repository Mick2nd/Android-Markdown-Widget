package ch.tiim.markdown_widget

import android.net.Uri
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.FileNotFoundException
import java.net.MalformedURLException
import java.net.URL

@RunWith(Parameterized::class)
class HttpTest {

    companion object {
        @JvmStatic
        @get:Parameterized.Parameters
        val data: Collection<Any> = listOf(
            arrayOf(
                "https://raw.githubusercontent.com/Mick2nd/Android-Markdown-Widget/main/README.md",
                "# Markdown Widget"
            ),
            arrayOf(
                "https://raw.githubusercontent.com/Mick2nd/Code-Section/master/README.md",
                "# Code Section Joplin Plug-in"
            )
        )
    }

    @Parameterized.Parameter(value = 0)
    lateinit var url: String
    @Parameterized.Parameter(value = 1)
    lateinit var content: String

    @Before
    fun setup() {

    }

    @Test
    fun uriExtensions_loadsWithCoroutine() {
        val text = URL(url).loadUrl()
        println("Loaded text from $url was ${text.substring(0 .. 40)}")
        assertTrue(text.startsWith(content))
    }

    @Test
    fun uriExtensions_throwsFileNotFound() {
        val exception = assertThrows(FileNotFoundException::class.java) {
            URL(url + "xxx").loadUrl()
        }
        println("$exception")
    }

    @Test
    fun uriExtensions_throwsIllegalProtocol() {
        val exception = assertThrows(MalformedURLException::class.java) {
            val url = URL(url.replace("https", "content"))
            url.loadUrl()
        }
        println("$exception")
    }

    @After
    fun teardown() {

    }
}
