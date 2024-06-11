package ch.tiim.markdown_widget

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Serves as Cache for file and internet content. The content access is now a one-time access. All
 * succeeding accesses are served by the cache. This implementation stores the cache in the
 * Shared Preferences of the app taking into account that the required size is not large.
 * A refresh button in the Main Activity window should *refresh* the cache **and** *update* the widgets.
 */
class ContentCacheImpl @Inject constructor(@ApplicationContext private val context: Context, private val prefs: Preferences) : ContentCache {

    /**
     * Loads data string given an uri. Either from Cache of (if not present) from the original
     * source.
     */
    override fun get(uri: Uri): String {
        val cachedContent = prefs["$uri", ""]
        if (cachedContent != "") {
            return cachedContent
        }
        return getNew(uri)
    }

    /**
     * Stores a [String] in the cache.
     */
    override fun set(uri: Uri, value: String) {
        prefs["$uri"] = value
    }

    /**
     * Loads data string from original source.
     */
    override fun getNew(uri: Uri): String {
        val text = uri.load(context)
        this[uri] = text
        return text
    }

    /**
     * Invalidates a single [Uri] by deleting its cache.
     */
    override fun invalidate(uri: Uri) {
        prefs.delete("$uri")
    }

    /**
     * Invalidates the whole cache.
     */
    override fun invalidateAll() {
        for (uri in prefs.uris()) {
            invalidate(uri)
        }
    }

    /**
     * Invalidates the cache and reloads the whole content.
     */
    override fun refresh() {
        for (uri in prefs.uris()) {
            getNew(uri)
        }
    }

    /**
     * Cleans the preferences from invalid Uris.
     */
    override fun clean() {
        if (prefs.uris().isNotEmpty()) {
            invalidateAll()
            refresh()
        }
    }
}

interface ContentCache {
    /**
     * Loads data string given an uri. Either from Cache of (if not present) from the original
     * source.
     */
    operator fun get(uri: Uri) : String

    /**
     * Stores a [String] in the cache.
     */
    operator fun set(uri: Uri, value: String)

    /**
     * Loads data string from original source.
     */
    fun getNew(uri: Uri) : String

    /**
     * Invalidates a single [Uri] by deleting its cache.
     */
    fun invalidate(uri: Uri)

    /**
     * Invalidates the whole cache.
     */
    fun invalidateAll()

    /**
     * Invalidates the cache and reloads the whole content.
     */
    fun refresh()

    /**
     * Cleans the preferences from invalid Uris.
     */
    fun clean()

}