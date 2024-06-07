package ch.tiim.markdown_widget

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader

private const val TAG = "Intercepted"
private const val TAG2 = "Not Intercepted"

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

                Log.d(TAG, "${request.url}")
                Log.d(TAG, r.mimeType)
                if (request.url.path == "/documents/userstyle.css") {
                    Log.d(TAG, "User Style Probe: ${read(r.data!!)}")
                }
                return r
            }

        } catch (err: Exception) {
            Log.w(TAG, "Non existence of ${request.url.path} simply suppressed: $err")
            return null
        }

        Log.d(TAG2, "${request.url}")
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
