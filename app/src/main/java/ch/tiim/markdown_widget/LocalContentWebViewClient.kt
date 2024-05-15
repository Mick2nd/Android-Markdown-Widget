package ch.tiim.markdown_widget

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.PathHandler
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader


/**
 * WebViewClient and WebViewClientCompat allowed as super class
 * Both of them work similar
 */
open class LocalContentWebViewClient(private val assetLoader: WebViewAssetLoader) : WebViewClient() {

    /**
     * Performs interception of Web Urls given the supporting assetLoader
     */
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ) : WebResourceResponse? {
        try {
            val r: WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
            if (r != null) {
                if (r.data == null || r.mimeType == null || r.statusCode == 404) {
                    throw FileNotFoundException("${request.url}")
                }

                Log.d("Intercepted", "${request.url}")
                Log.d("Intercepted", r.mimeType)
                if (request.url.path == "/documents/userstyle.css") {
                    Log.d("Intercepted", "User Style Probe: ${read(r.data!!)}")
                }
                return r
            }

        } catch (err: Exception) {
            Log.w("Intercepted", "Non existence of ${request.url.path} simply suppressed: $err", err)
            return null;
        }

        Log.d("Not Intercepted", "${request.url}")
        return super.shouldInterceptRequest(view, request)
    }

    /**
     * Fun is intended to extract some content at the beginning of a stream
     */
    private fun read(stream: InputStream): String {
        if (!stream.markSupported()) {
            return "MARK NOT SUPPORTED"
        }

        stream.mark(0x10000)
        val r = BufferedReader(InputStreamReader(stream))
        var x: String = ""

        for (c in 1 .. 5) {
            x += r.readLine()
        }
        stream.reset()

        return x
    }
}
