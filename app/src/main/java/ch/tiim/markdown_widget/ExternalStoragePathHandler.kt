package ch.tiim.markdown_widget

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "ExternalStoragePathHandler"

/**
 * TRIAL to use external (shared) storage for the userstyle.css file
 * SUCCEEDED
 * @param context the context
 * @param subFolder here the sub folder of the files folder
 */
@Singleton
class ExternalStoragePathHandler @Inject constructor(
    private val context: Context,
    @Named("SUBFOLDER") private val subFolder: String
    ) : WebViewAssetLoader.PathHandler {

    /**
     * Implements the PathHandler interface
     * @param path file name of the intercepted file
     * @return a web resource response
     */
    override fun handle(path: String): WebResourceResponse {
        try {
            val dir = context.getExternalFilesDir(subFolder)
            dir?.mkdirs()
            val file = File(dir, path)
            val uri = Uri.fromFile(file)
            val stream = context.contentResolver.openInputStream(uri)
            return WebResourceResponse("text/css", "utf-8", stream)
        } catch (err: Exception) {
            throw err
        }
    }
}

/**
 * Goal of this class is to support access of WebView to files in SHARED EXTERNAL STORAGE
 * Implemented as SINGLETON
 */
@Singleton
class ExternalStoragePathHandlerAlt @Inject constructor(
    private val context: Context,
    private val prefs: Preferences) : WebViewAssetLoader.PathHandler {

    /**
     * Implements the PathHandler interface
     * @param path file name of the intercepted file
     * @return a web resource response
     */
    override fun handle(path: String): WebResourceResponse {
        try {
            val stream = getInputStream(path)
            return WebResourceResponse("text/css", "utf-8", stream)
        } catch (err: Exception) {
            throw err
        }

        // return WebResourceResponse(null, null, null)
    }

    /**
     * Intentionally opens and returns an Input Stream
     * @param path the filename of the document to open
     * @return the opened Input Stream
     */
    private fun getInputStream(path: String) : InputStream {
        val uri = prefs[path]
        val stream = (context.contentResolver).openInputStream(uri)
        Log.i(TAG, "Opened Input Stream at $uri: ${stream != null}")
        return stream!!
    }
}
