package ch.tiim.markdown_widget.fakes

import android.content.Context
import ch.tiim.markdown_widget.PREF_FILE
import ch.tiim.markdown_widget.PREF_PREFIX_KEY
import ch.tiim.markdown_widget.Preferences

/**
 * Preferences override. Only the base indexer is overwritten. The remaining functionality is as is.
 * Alternative approach could be to use Mock objects to intercept access to SharedPreferences.
 */
class FakePreferences(private val context: Context) : Preferences(context) {

    private val mutableMap: MutableMap<String, String> = hashMapOf( "${PREF_PREFIX_KEY}1--$PREF_FILE" to "x123" )

    override operator fun get(prefName: String, default: String) : String {
        mutableMap[prefName]?.let { return it }
        return default
    }

    override operator fun set(prefName: String, value: String) {
        mutableMap[prefName] = value
    }
}
