package ch.tiim.markdown_widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
 * @param data the md string to be rendered
 * @param widthRatio the ratio between widget width and screen width
 * @param onReady a callback to be invoked when [WebView] becomes ready
 */
class MarkdownRenderer @Inject constructor(
    private val context: Context,
    private val data: String,
    private var widthRatio: Float = 1.0f,
    private var onReady: (() -> Unit) = {  }
) {
    var webView: WebView? = null

    @Inject lateinit var changeObserver: ChangeObserver
    @Inject lateinit var prefs: Preferences
    @Inject @Named("GLOBAL") lateinit var pathHandlerAlt: WebViewAssetLoader.PathHandler
    @Inject @Named("EXTERNAL") lateinit var pathHandler: WebViewAssetLoader.PathHandler

    private var bitmap: Bitmap? = null
    private val theme = ""
    private var ready = false
    private var time: Long = 0
    private var html = ""

    /**
     * Init block. Performs first time rendering. Responsible for DI per Dagger too.
     */
    init {
        EntryPoints.get(context.applicationContext, CustomEntryPoint::class.java).inject(this)
        changeObserver.updateState(data, widthRatio)

        time = System.currentTimeMillis()
        html = getHtml(data)
        val duration = System.currentTimeMillis() - time
        Log.d(TAG, "Duration of Html rendering: ${duration}ms")
        prepareWebView(html)
    }

    /**
     * Refreshes the [WebView] inside an existing instance.
     */
    fun refresh(widthRatio: Float, onReady: (() -> Unit)) {
        if (!isReady()) {
            return
        }
        this.widthRatio = widthRatio
        this.onReady = onReady
        ready = false
        prepareWebView(html)

        // Seems to be not so reliable:
        // webView?.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        // webView?.reload()
    }

    /**
     * Queries the [Bitmap] of the rendered markdown.
     */
    fun getBitmap(width: Int, height: Int): Bitmap {
        if (!isReady()) {
            Log.e(TAG, "WebView is not ready yet!")
            return createDummy(width, height)
        }

        if (bitmap == null) {
            bitmap = webView?.drawBitmap(width, height)
        }
        return bitmap!!.extractBitmap(0, 0, width, height)
    }

    /**
     * Checks for markdown change.
     *
     * @param s a new md string
     */
    fun needsMarkdownUpdate(s: String) : Boolean {
        return changeObserver.needsMarkdownUpdate(s)
    }

    /**
     * Queries if an update of a [MarkdownFileWidget] is required. This depends on the states of
     * the md string and the *userstyle.css* file.
     *
     * @param widthRatio a new ratio for widget width
     * @return flag indicating whether an update is required
     */
    fun needsUpdate(widthRatio: Float) : Boolean {
        return changeObserver.needsRefresh(widthRatio)
    }

    /**
     * Renders the given html into the [WebView]. On completion invokes the onReady callback.
     *
     * @param html html to be rendered. This will be injected into the complete html document *index.html*
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun prepareWebView(
        html: String
    ) {
        time = System.currentTimeMillis()
        // WebView.enableSlowWholeDocumentDraw()                                                    // ?
        webView = WebView(context)
        bitmap = null                                                                               // as indication that it must be drawn

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
            it.layout(0, 0, prefs[SCREEN_WIDTH, "1000"].toInt(), prefs[SCREEN_HEIGHT, "1000"].toInt())
            val fallback = "<div style=\"color: red; font-size: 2em;\">No content to display!</div>"
            val preparedHtml = if (html != "") html else fallback
            it.addJavascriptInterface(JsObject(theme, preparedHtml, prefs.zoom, widthRatio), "jsObject")
            it.loadUrl("https://appassets.androidplatform.net/assets/index.html")
            Log.i(TAG, "WebView instance created and Html loaded: ${html.begin()}")
        }
    }

    /**
     * [toString] override with some special information.
     */
    override fun toString() : String {
        val cls = super.toString().substringAfterLast('.')
        return "$cls : md : ${data.substring(0, 20)}, html : ${html.substring(0, 20)}"
    }

    /**
     * Queries for the ready state of the rendering.
     */
    private fun isReady() : Boolean {
        return ready && webView != null && webView!!.contentHeight != 0
    }

    /**
     * Draws a Bitmap from WebView content. The complete content is drawn, defined by contentHeight.
     */
    private fun WebView.drawBitmap(width: Int, height: Int) : Bitmap {
        val time = System.currentTimeMillis()
        val contentHeightPixels = (contentHeight * Resources.getSystem().displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(width, contentHeightPixels, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        val duration = System.currentTimeMillis() - time
        Log.i(TAG, "$bitmap $width x $contentHeightPixels, execution in ${duration}ms")

        return bitmap
    }

    /**
     * Creates a dummy if the WebView is not ready yet.
     */
    private fun createDummy(width: Int, height: Int) : Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.YELLOW)
        return bitmap
    }

    /**
     * Extracts a part of a greater Bitmap.
     * Usage Recipe:
     * - build a greater Bitmap, a part of which is to be extracted.
     * - define location and part
     */
    private fun Bitmap.extractBitmap(x: Int, y: Int, width: Int, height: Int): Bitmap {
        val wl = width.coerceIn(0, this.width)
        val hl = height.coerceIn(0, this.height)
        val xl = x.coerceIn(0, this.width - wl)
        val yl = y.coerceIn(0, this.height - hl)

        val newBitmap = Bitmap.createBitmap(wl, hl, Bitmap.Config.ARGB_8888)        // Create a same size Bitmap
        val pixels = IntArray(wl * hl)
        getPixels(pixels, 0, wl, xl, yl, wl, hl)
        newBitmap.setPixels(pixels, 0, wl, 0, 0, wl, hl)
        return newBitmap
    }

    /**
     * Given the md string this parses it into a html string
     *
     * @param markdown the md string
     * @return the ready to be used html string
     */
    private fun getHtml(markdown: String): String {
        val mdParser = MarkdownParser(theme)
        return mdParser.parse(markdown)
    }

    /**
     * This object allows injection of information into the index.html file as js object.
     *
     * @property theme a theme string
     * @property html the html created from markdown
     * @property zoom the zoom factor to apply (scale transform of web page)
     * @property widthRatio the width ratio to calculate the real width
     */
    class JsObject (val theme: String, val html: String, val zoom: Float = 0.7f, private val widthRatio: Float = 1.0f)
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

        /**
         * In Js we can query for the zoom factor.
         */
        @JavascriptInterface
        fun injectZoom() : Float {
            return zoom
        }

        /**
         * In Js we can query for the width ratio between widget width and screen width.
         */
        @JavascriptInterface
        fun injectWidthRatio() : Float {
            return widthRatio
        }
    }
}
