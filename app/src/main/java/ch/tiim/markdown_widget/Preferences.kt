package ch.tiim.markdown_widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException

internal const val PREF_FILE = "filepath"
internal const val PREF_BEHAVIOUR = "behaviour"
internal const val ENCODED_FOLDER_URI = "encodedFolderUri"
internal const val SCREEN_WIDTH = "ScreenWidth"
internal const val SCREEN_HEIGHT = "ScreenHeight"

private const val PREF_PREFIX_KEY = "appwidget_"
private const val PREFS_NAME = "ch.tiim.markdown_widget.MarkdownFileWidget"

private const val TAG = "Preferences"

/**
 * Supports reading/writing from/to SharedPreferences object.
 * Here means:
 * - global: application wide setting
 * - individual: setting related to given app widget (by its id)
 */
open class Preferences(@ApplicationContext private val context: Context) {

    private val cache = HashMap<String, String>()

    init {
        Log.d(TAG, "Preferences instantiated $this")
    }

    /**
     * Clears the cache. The stored settings should be available anyway.
     */
    fun clearCache() {
        cache.clear()
    }

    /**
     * Read "Global" preference
     * @param prefName the global preference name
     * @param default the default value
     * @return the read preference
     */
    open operator fun get(prefName: String, default: String) : String {
        val internalPref = "$PREF_PREFIX_KEY$prefName"
        synchronized(this) {
            if (internalPref !in cache) {
                val prefs = context.getSharedPreferences(PREFS_NAME, 0)
                val value = prefs.getString(internalPref, default)
                cache[internalPref] = value ?: default
            }
            cache[internalPref]?.let {
                return it
            }
            return ""
        }
    }

    /**
     * Read preference individual for given appWidgetId
     * The global preference name will be calculated
     *
     * @param appWidgetId the id of the appwidget
     * @param prefName the "local" preference name
     * @param default default
     * @return the read preference
     */
    operator fun get(appWidgetId: Int, prefName: String, default: String) : String {
        return this["$appWidgetId--$prefName", default]
    }

    /**
     * Reads Markdown file path from setting, converts to [Uri] and returns it.
     *
     * @param appWidgetId the id of the app widget
     * @return the calculated [Uri]
     */
    operator fun get(appWidgetId: Int) : Uri {
        return Uri.parse(this[appWidgetId, PREF_FILE, ""])
    }

    /**
     * Same as [get] above.
     */
    fun markdownUriOf(appWidgetId: Int) : Uri = this[appWidgetId]

    /**
     * Builds and returns the [Uri] for the given file path. The location is the configured
     * User Folder Uri as requested from the user.
     * There is a single use case of this: the userstyle.css file permitting the user to provide
     * his own styling.
     *
     * @param path the file path, most probably the userstyle.css file
     * @return the calculated [Uri]
     */
    operator fun get(path: String) : Uri {
        return DocumentsContract.buildDocumentUriUsingTree(this.userFolderUri, documentIdOf(path))
    }

    /**
     * Same as [get] above.
     */
    fun userDocumentUriOf(path: String) : Uri = this[path]

    /**
     * Write "Global" preference
     * @param prefName the global preference name
     * @param value the value to be written
     */
    open operator fun set(prefName: String, value: String) {
        val internalPref = "$PREF_PREFIX_KEY$prefName"
        synchronized(this) {
            if (internalPref !in cache || cache[internalPref] != value) {
                cache[internalPref] = value
                val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
                prefs.putString(internalPref, value)
                prefs.apply()
            }
        }
    }

    /**
     * Write preference individual for given appWidgetId
     * The global preference name will be calculated
     * @param appWidgetId the id of the app widget
     * @param prefName the "local" preference name
     * @param value the value to be written
     */
    operator fun set(appWidgetId: Int, prefName: String, value: String) {
        this["$appWidgetId--$prefName"] = value
    }

    /**
     * Used to store the md file's path given its [Uri]
     *
     * @param appWidgetId id of the app widget
     * @param uri [Uri] of the md file
     */
    operator fun set(appWidgetId: Int, uri: Uri) {
        this[appWidgetId, PREF_FILE] = uri.toString()
    }

    fun setMarkdownUri(appWidgetId: Int, uri: Uri) { this[appWidgetId] = uri }

    /**
     * Deletes an individual global preference
     * @param prefName the global name
     */
    fun delete(prefName: String) {
        val internalPref = "$PREF_PREFIX_KEY$prefName"
        synchronized(this) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
            prefs.remove(internalPref)
            cache.remove(internalPref)
            prefs.apply()
        }
    }

    /**
     * Deletes preferences used by an individual widget
     * @param appWidgetId the id of the app widget
     */
    fun delete(appWidgetId: Int) {
        val keys = listOf("$appWidgetId--$PREF_BEHAVIOUR", "$appWidgetId--$PREF_FILE")
        for (key in keys) {
            delete(key)
        }
    }

    /**
     * User Folder Uri as getter / setter implementation.
     * This setting is backed by a cached Preference.
     * Only one instance may exist for this mechanism to work.
     */
    var userFolderUri : Uri?
        get() {
            val encodedUri = this[ENCODED_FOLDER_URI, ""]
            if (encodedUri != "") {
                val uri = Uri.parse(Uri.decode(encodedUri))
                Log.d(TAG, "Decoded Folder Uri from app state: $uri")
                return uri
            }
            // TODO: should return null for dependent code to work
            // does test code depend on this?
            return null // Uri.Builder().build()
        }
        set(value) {
            if (value == null)
                return
            this[ENCODED_FOLDER_URI] = Uri.encode(value.toString())
            Log.d(TAG, "Stored encoded Folder Uri to app state")
        }

    var useUserStyle : Boolean
        get() {
            return this["useUserStyle", "false"].toBoolean()
        }
        set(value) {
            this["useUserStyle"] = "$value"
        }

    var zoom : Float
        get() {
            return this["zoom", "0.7"].toFloat()
        }
        set(value) {
            this["zoom"] = "$value"
        }

    /**
     * Revokes the folder permission thus forcing a new request on next restart
     */
    fun revokeUserFolderPermission() {
        try {
            userFolderUri?.let {
                context.contentResolver.releasePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        catch(err: Exception) { }
        delete(ENCODED_FOLDER_URI)
    }

    /**
     * Builds and returns a list of all Shared Preferences by key.
     */
    fun keys(predicate: (String)->Boolean = { s -> true}) : List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        return prefs.all.keys.filter { s -> predicate(s) }.map { it.substring(PREF_PREFIX_KEY.length) }
    }

    /**
     * Builds and returns a list of all Shared Preferences containing a Uri as key.
     */
    fun uris() =
        keys()
        .filter { it.contains("://") }
        .map { Uri.parse(it) }

    /**
     * Builds and returns a list of all Shared Preferences containing a widgetId as key. (distinct
     * ids only)
     */
    fun widgetIds() =
        keys()
        .filter { it[0].isDigit() }
        .map { it.replace(Regex("(\\d+)[^0-9].*"), "$1").toInt() }
        .distinct()

    /**
     * This query has the task of providing a document id for a given display name
     * The file in question must exist in the folder with uri folderUri
     * @param path the display name of the file or file name
     * @return the document id
     * @throws FileNotFoundException
     */
    private fun documentIdOf(path: String) : String {
        val userFolderUri = this.userFolderUri
        val treeUri = DocumentsContract.buildChildDocumentsUriUsingTree(userFolderUri, DocumentsContract.getTreeDocumentId(userFolderUri))
        val cursor = context.contentResolver.query(
            treeUri,
            arrayOf(OpenableColumns.DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            "${OpenableColumns.DISPLAY_NAME} LIKE ?",
            arrayOf(path),
            "ASC")
        while (cursor != null && cursor.moveToNext()) {                                             // selection not working -> THIS SOLUTION
            if (cursor.getString(0) == path) {
                val documentId = cursor.getString(1)
                cursor.close()
                return documentId
            }
        }

        cursor?.close()
        // return ""
        // NOT COMPATIBLE WITH MOCKITO
        throw FileNotFoundException("Document $path not found")
    }

    /**
     * Test - outputs the column names.
     */
    private fun test(uri: Uri) {
        val cursor = context.contentResolver.query(
            uri,
            null,
            null,
            null,
            null)
        for (name in cursor?.columnNames ?: arrayOf()) {
            Log.d(TAG, name)
        }
        cursor?.close()
    }
}
