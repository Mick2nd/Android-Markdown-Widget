package ch.tiim.markdown_widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import android.webkit.WebView.RENDERER_PRIORITY_BOUND
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import ch.tiim.markdown_widget.di.CustomEntryPoint
import dagger.hilt.EntryPoints
import java.io.File
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "MarkdownRenderer"

/**
 * Renders a given html string into a [WebView] and returns [Bitmap] copies of it.
 *
 * @param context the context
 * @param width the width
 * @param height the height
 * @param data the md string to be rendered
 * @param onReady a callback to be invoked when [WebView] becomes ready
 */
class MarkdownRenderer @Inject constructor(
    private val context: Context,
    private val width: Int = 0,
    private val height: Int = 0,
    private val data: String,
    private var onReady: (() -> Unit) = {  }
) {
    var webView: WebView? = null

    @Inject lateinit var fileChecker: FileServices
    @Inject @Named("GLOBAL") lateinit var pathHandlerAlt: WebViewAssetLoader.PathHandler
    @Inject @Named("EXTERNAL") lateinit var pathHandler: WebViewAssetLoader.PathHandler
    private val theme = ""
    private var ready = false
    private var time: Long = 0
    private var html = ""

    /**
     * Init block. Performs first time rendering. Responsible for DI per Dagger too.
     */
    init {
        EntryPoints.get(context.applicationContext, CustomEntryPoint::class.java).inject(this)
        time = System.currentTimeMillis()

        html = getHtml(data)
        val duration = System.currentTimeMillis() - time
        Log.d(TAG, "Duration of Html rendering: ${duration}ms")
        prepareWebView(html)
    }

    /**
     * Refreshes the [WebView] inside an existing instance.
     */
    fun refresh(onReady: (() -> Unit)) {
        this.onReady = onReady
        ready = false
        prepareWebView(html)

        // Seems to be not so reliable:
        // webView?.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        // webView?.reload()
    }

    /**
     * Queries for the ready state of the rendering.
     */
    fun isReady() : Boolean {
        return ready && webView!!.contentHeight != 0 && !fileChecker.stateChanged
    }

    /**
     * Queries the [Bitmap] of the rendered markdown.
     */
    fun getBitmap(width: Int, height: Int): Bitmap {
        if (!isReady()) {
            Log.e(TAG, "WebView is not ready yet!")
        }

        Log.d(TAG, "Here in getBitmap")
        val time = System.currentTimeMillis()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        webView!!.draw(canvas)
        val duration = System.currentTimeMillis() - time
        Log.i(TAG, "$bitmap, execution in ${duration}ms")
        return bitmap
    }

    /**
     * Queries if an update of a [MarkdownFileWidget] is required. This depends on the states of
     * the md string and the *userstyle.css* file.
     *
     * @param s a new md string
     * @return flag indicating whether an update is required
     */
    fun needsUpdate(s: String) : Boolean {
        return this.data != s || fileChecker.stateChanged
    }

    /**
     * Renders the given html into the [WebView]. On completion invokes the onReady callback.
     *
     * @param html html to be rendered. This will be injected into the complete html document *index.html*
     */
    fun prepareWebView(
        html: String
    ) {
        time = System.currentTimeMillis()
        webView = WebView(context)

        /**
         * The Asset Loader implements an interception mechanism for web site content:
         * - with an **assets** sub folder one can load all files in the app assets folder
         * - with **public** one can load files from the internal app folder under files/public
         * - with **documents** one can load files from app specific external folder
         * - with **documents-public** one can load files from the GLOBAL external documents folder.
         *   The permission to this folder is requested from the user to gain access to it.
         *
         * At the moment only one file is outside the assets folder: the user supplied **userstyle.css**
         * file at **documents-public**. It can be used by a user to fine-style its widgets
         */
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", AssetsPathHandler(context))
            .addPathHandler("/public/", InternalStoragePathHandler(context, File(context.filesDir, "public")))
            .addPathHandler("/documents/", pathHandler)

            // THIS HANDLER NEEDS EXTRA INJECTION OUTSIDE THIS CODE
            .addPathHandler("/documents-public/", pathHandlerAlt)
            .build()

        webView!!.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // THIS CODE IS CRUCIAL FOR PROPER UPDATE OF APP WIDGETS
                it.setRendererPriorityPolicy(RENDERER_PRIORITY_BOUND, true)
            }

            it.webViewClient = object : LocalContentWebViewClient(assetLoader) {

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Markdown: $html")
                    fileChecker.updateState()

                    val duration = System.currentTimeMillis() - time
                    Log.d(TAG, "Duration of Web Site display: ${duration}ms")
                    ready = true
                    onReady()
                }
            }

            with(it.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = LOAD_NO_CACHE
            }

            it.clearHistory()
            it.clearCache(true)
            val w = if (width != 0) width else 900
            val h = if (height != 0) height else 1200
            it.layout(0, 0, w, h)
            it.addJavascriptInterface(JsObject(theme, html), "jsObject")
            it.loadUrl("https://appassets.androidplatform.net/assets/index.html")
            Log.i(TAG, "WebView instance created and Html loaded: $html")
        }
    }

    /**
     * Given the md string this parses it into a html string
     *
     * @param data the md string
     * @return the ready to be used html string
     */
    private fun getHtml(data: String): String {
        val mdParser = MarkdownParser(theme)
        return mdParser.parse(data)
    }

    /**
     * This object allows injection of information into the index.html file as js object
     *
     * @property theme a theme string
     * @property html the html created from markdown
     */
    class JsObject (val theme: String, val html: String)
    {
        /**
         * Used inside Js code to inject a theme string.
         */
        @JavascriptInterface
        fun injectTheme() : String {
            return theme
        }

        /**
         * In Js we can query for the rendered markdown to be injected into the html body.
         */
        @JavascriptInterface
        fun injectHtml() : String {
            return html
        }
    }
}

/**
* Executes the given [block] and returns elapsed time in milliseconds.
*/
inline fun measureTimeMillis(block: () -> Unit): Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}