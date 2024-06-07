package ch.tiim.markdown_widget

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

private const val TAG = "FileServices"

/**
 * This class is meant as checker for the *userstyle.css* file changes.
 * The check has the same relevance as the check of Markdown changes.
 * The location of this file is configured by the [uri].
 *
 * @param context the context used to get a contentResolver
 * @param uri the complete Uri of the file to be checked
 */
@Singleton
class FileServices @Inject constructor (private val context: Context, private val uri: Uri) {

    var content: String = ""
    private var err: Exception? = null

    /**
     * Init block, loads the file defined by its uri.
     */
    init {
        Log.i(TAG, "File Checker $this instantiated with uri $uri")
        updateState()
    }

    /**
     * Updates the file content.
     */
    fun updateState() {
        content = loadFile()
    }

    /**
     * Checks if state (content) has changed.
     */
    val stateChanged
        get(): Boolean {
            val content = loadFile()
            val result = content != this.content
            this.content = content
            return result
        }

    /**
     * Loads file from [Uri] and returns content. This may be a file from Internet, identified by
     * the scheme property.
     */
    private fun loadFile(): String {
        try {
            if (uri.scheme in arrayOf("http", "https")) {
                return URL(uri.toString()).getText()
            }

            val ins: InputStream = context.contentResolver.openInputStream(uri)!!
            val reader = BufferedReader(InputStreamReader(ins, "utf-8"))
            val data = reader.lines().reduce { s, t -> s + "\n" + t }
            reader.close()
            ins.close()
            err = null
            return data.get()
        } catch (err: FileNotFoundException) {
            this.err = err
            Log.w(TAG, err.toString())
            return ""
        } catch (err: Exception) {
            this.err = err
            Log.w(TAG, err.toString())
            return ""
        } finally {
        }
    }

    /**
     * Loads an Internet page. Extension method of [URL]
     */
    private fun URL.getText(): String {

        var text = ""
        thread {
            text = openConnection().run {
                this as HttpURLConnection
                val t = inputStream.bufferedReader().readText()
                inputStream.bufferedReader().close()
                inputStream.close()
                disconnect()
                Log.d(TAG, "HTTP(S) content loaded: $t")
                t
            }
        }.join()
        return text
    }
}
