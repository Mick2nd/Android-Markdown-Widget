package ch.tiim.markdown_widget

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

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

    val stateChanged
        get(): Boolean {
            return loadFile() != content
        }

    private fun loadFile(): String {
        try {
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
}
