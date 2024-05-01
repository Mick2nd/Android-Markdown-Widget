package ch.tiim.markdown_widget

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.math.max
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import java.io.File

private const val TAG = "MarkdownRenderer"
class MarkdownRenderer(private val context: Context, private val width: Int, private val height: Int, private val data: String, private val onReady: ((Bitmap) -> Unit) = {}) {

    val file: File = File(context.filesDir, "public/userstyle.css")
    val uri: Uri = Uri.fromFile(file)
    val fileChecker: FileChecker = FileChecker(context, uri)

    private val theme = ""
    private val mdParser = MarkdownParser(theme)
    val webView = WebView(context)
    private val bitmap = Bitmap.createBitmap(max(width, 100), max(height, 100), Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)
    private var ready = false

    init {
        val html = getHtml(data)
        prepareWebView(html)
    }

    private fun prepareWebView(
        html: String
    ) {

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", AssetsPathHandler(context))
            .addPathHandler("/public/", InternalStoragePathHandler(context, File(context.filesDir, "public")))
            .build()

        webView.webViewClient = object: LocalContentWebViewClient(assetLoader) {
            override fun onPageFinished(view: WebView?, url: String?) {

                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed(Runnable {
                    try {
                        Log.d(TAG, "Markdown: $html")
                        webView.draw(canvas)
                        fileChecker.updateState()
                        ready = true
                        onReady(bitmap)

                    } catch (e: PendingIntent.CanceledException) {
                        e.printStackTrace()
                        Log.e(TAG, e.toString())
                    }
                }, 50)
                super.onPageFinished(view, url)
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        webView.layout(0, 0, width, height)
        // webView.setBackgroundColor(Color.WHITE)
        webView.addJavascriptInterface(JsObject(theme, html), "jsObject")
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        Log.i(TAG, "New MD Renderer instance created and Html loaded")
    }

    fun isReady():Boolean {
        return ready && webView.contentHeight != 0 && !fileChecker.stateChanged()
    }

    fun getBitmap(): Bitmap {
        if (!isReady()) {
            Log.e(TAG, "WebView is not ready yet!")
        }
        Log.i(TAG, bitmap.toString())
        return bitmap
    }

    private fun getHtml(data: String):String {
        return mdParser.parse(data)
    }

    fun needsUpdate(width: Int, height: Int, s: String):Boolean {
        return this.width != width || this.height != height || this.data != s
    }
}

class JsObject (val theme: String, val html: String)
{
    @JavascriptInterface
    public fun injectTheme() : String {
        return theme
    }
    @JavascriptInterface
    public fun injectHtml() : String {
        return html
    }
}
