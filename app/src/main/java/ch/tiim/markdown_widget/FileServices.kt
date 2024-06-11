package ch.tiim.markdown_widget

import android.content.Context
import android.net.Uri
import android.util.Log
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

    private var content: String = ""

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
        content = load()
    }

    /**
     * Checks if state (content) has changed.
     */
    val stateChanged
        get(): Boolean {
            val content = load()
            val result = content != this.content
            this.content = content
            return result
        }

    /**
     * Encapsulates uri.load thus preventing to throw.
     */
    private fun load() : String {
        return try {
            uri.load(context)
        } catch (err: Throwable) {
            Log.w(TAG, "Exception in FileServices.load $err")
            ""
        }
    }
}
