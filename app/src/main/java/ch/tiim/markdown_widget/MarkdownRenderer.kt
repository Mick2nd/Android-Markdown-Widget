package ch.tiim.markdown_widget

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.math.max
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import android.webkit.ValueCallback


private const val TAG = "MarkdownRenderer"
class MarkdownRenderer(private val context: Context, private val width: Int, private val height: Int, private val data: String, private val onReady: ((Bitmap) -> Unit) = {}) {

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
            .addPathHandler("/assets/", AssetsPathHandler(context)).build()
        webView.webViewClient = object: LocalContentWebViewClient(assetLoader) {
            override fun onPageFinished(view: WebView?, url: String?) {

                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed(Runnable {
                    try {
                        Log.d("Test1:", html)
                        val js = """
                            try {
                                var th = document.getElementById("theme");
                                th.innerHTML = `$theme`;
                                var md = document.getElementById("markdown");
                                md.innerHTML = `${html.replace("\\", "&bsol;")}`;
                            }
                            catch(e) {
                                console.log('Error:' , e.message);
                            }                            
                        """.trimIndent()
                        try {
                            webView.evaluateJavascript(js, null)
                            webView.loadUrl("javascript:renderKatex();")
                            webView.loadUrl("javascript:renderMermaid();")
                            webView.loadUrl("javascript:admonitionHelper();")
                        }
                        catch (err: Exception) {
                            err.printStackTrace()
                        }

                        webView.draw(canvas)
                        ready = true
                        onReady(bitmap)
                    } catch (e: PendingIntent.CanceledException) {
                        e.printStackTrace()
                    }
                }, 50)
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        webView.layout(0, 0, width, height)
        webView.setBackgroundColor(Color.WHITE)
        // webView.setBackgroundColor(Color.MAGENTA)
        // val encodedHtml = Base64.encodeToString(html.toByteArray(), Base64.DEFAULT)
        // webView.loadData(encodedHtml, "text/html", "base64")
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        webView.isDrawingCacheEnabled = true
        webView.buildDrawingCache()
    }

    fun isReady():Boolean {
        return ready && webView.contentHeight != 0
    }

    fun getBitmap(): Bitmap {
        if (!ready) {
            Log.e(TAG, "WebView is not ready yet!")
        }
        return bitmap
    }

    private fun getHtml(data: String):String {
        return mdParser.parse(data)
    }

    fun needsUpdate(width: Int, height: Int, s: String):Boolean {
        return this.width != width || this.height != height || this.data != s
    }
}