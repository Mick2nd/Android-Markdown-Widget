package ch.tiim.markdown_widget

import android.content.Context
import android.net.Uri
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ChangeObserver"

/**
 * This class is meant as checker for the *userstyle.css* file changes. The check has the same
 * relevance as the check of Markdown changes. The location of this file is configured by the [uri].
 *
 * @param context the context used to get a contentResolver
 * @param uri the complete Uri of the file to be checked
 */
class ChangeObserver @Inject constructor (private val context: Context, private val uri: Uri, private val prefs: Preferences) {

    private var markdown = ""
    private var widthRatio = 0.9f
    private var userStyle: String = ""
    private var useUserStyle = false
    private var zoom = 0.7f

    /**
     * Init block, loads the file defined by its uri.
     */
    init {
        Log.i(TAG, "Change Observer $this instantiated with uri $uri")
        updateState()
    }

    /**
     * Checks if markdown has changed.
     */
    fun needsMarkdownUpdate(markdown: String) : Boolean {
        val result = markdown != this.markdown
        this.markdown = markdown
        updateState()
        return result
    }

    /**
     * Checks if render settings have changed.
     */
    fun needsRefresh(widthRatio: Float) : Boolean {
        val result = prefs.useUserStyle != useUserStyle || prefs.zoom != zoom || widthRatio != this.widthRatio || load() != userStyle
        this.widthRatio = widthRatio
        updateState()
        return result
    }

    /**
     * Updates the file content.
     */
    fun updateState(markdown: String = "", widthRatio: Float = -1.0f) {
        userStyle = load()
        useUserStyle = prefs.useUserStyle
        zoom = prefs.zoom
        if (markdown.isNotEmpty()) {
            this.markdown = markdown
        }
        if (widthRatio > 0.0f) {
            this.widthRatio = widthRatio
        }
    }

    /**
     * Encapsulates uri.load thus preventing to throw.
     */
    private fun load() : String {
        return try {
            uri.load(context)
        } catch (err: Throwable) {
            Log.w(TAG, "Exception in ChangeObserver.load $err")
            ""
        }
    }
}
