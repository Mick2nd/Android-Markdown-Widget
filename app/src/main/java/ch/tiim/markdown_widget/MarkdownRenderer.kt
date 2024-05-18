package ch.tiim.markdown_widget

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import android.webkit.WebView.RENDERER_PRIORITY_BOUND
import android.webkit.WebViewClient
import androidx.webkit.ServiceWorkerWebSettingsCompat.CacheMode
import androidx.webkit.WebResourceErrorCompat
import kotlin.math.max
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import ch.tiim.markdown_widget.di.AppComponent
import ch.tiim.markdown_widget.di.DaggerAppComponent
import java.io.File

private const val TAG = "MarkdownRenderer"
class MarkdownRenderer(
    private val context: Context,
    private val width: Int = 0,
    private val height: Int = 0,
    private val data: String,
    private var onReady: (() -> Unit) = {  }
) {
    var webView: WebView? = null

    private val file: File = File(context.filesDir, "public/userstyle.css")
    private val uri: Uri = Uri.fromFile(file)
    private val fileChecker: FileChecker = FileChecker(context, uri)

    private val theme = ""
    private var ready = false
    private var time: Long = 0
    private var html = ""

    init {
        time = System.currentTimeMillis()
        html = getHtml(data)
        val duration = System.currentTimeMillis() - time
        Log.d(TAG, "Duration of Html rendering: ${duration}ms")
        prepareWebView(html)
    }

    private fun prepareWebView(
        html: String
    ) {
        time = System.currentTimeMillis()
        webView = WebView(context)

        /**
         * The Asset Loader implements an interception mechanism for web site content
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
            .addPathHandler("/documents/", ExternalStoragePathHandler(context, Environment.DIRECTORY_DOCUMENTS))

            // THIS HANDLER NEEDS EXTRA INJECTION OUTSIDE THIS CODE
            .addPathHandler("/documents-public/", AppComponent.instance.externalStoragePathHandler() as WebViewAssetLoader.PathHandler)
            .build()

        webView!!.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.setRendererPriorityPolicy(RENDERER_PRIORITY_BOUND, true)
            }

            it.webViewClient = object : LocalContentWebViewClient(assetLoader) {
                init {
                    Log.d(TAG, "INIT of webViewClient")
                }

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

    fun refresh(onReady: (() -> Unit)) {
        this.onReady = onReady
        ready = false
        prepareWebView(html)

        // Seems to be not so reliable:
        // webView?.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        // webView?.reload()
    }

    fun isReady():Boolean {
        return ready && webView!!.contentHeight != 0 && !fileChecker.stateChanged()
    }

    fun getBitmap(width: Int, height: Int): Bitmap {
        if (!isReady()) {
            Log.e(TAG, "WebView is not ready yet!")
        }

        Log.d(TAG, "Here in getBitmap")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        webView!!.draw(canvas)
        Log.i(TAG, "$bitmap, execution in ${time}ms")
        return bitmap
    }

    private fun getHtml(data: String): String {
        val mdParser = MarkdownParser(theme)
        return mdParser.parse(data)
    }

    fun needsUpdate(s: String):Boolean {
        return this.data != s || fileChecker.stateChanged()
    }
}

/**
 * This object allows injection of information into the index.html file as js object
 * @property theme a theme string
 * @property html the html created from markdown
 */
class JsObject (val theme: String, val html: String)
{
    @JavascriptInterface
    public fun injectTheme() : String {
        return theme
    }

    /**
     * In Js we can query for the rendered markdown to be injected into the html body
     */
    @JavascriptInterface
    public fun injectHtml() : String {
        return html
    }
}

/**
* Executes the given [block] and returns elapsed time in milliseconds.
*/
public inline fun measureTimeMillis(block: () -> Unit): Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}