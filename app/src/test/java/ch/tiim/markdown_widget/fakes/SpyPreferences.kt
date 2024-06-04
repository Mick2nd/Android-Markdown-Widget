package ch.tiim.markdown_widget.fakes

import android.content.Context
import android.net.Uri
import ch.tiim.markdown_widget.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mockito.Mockito
import org.mockito.Mockito.spy
import org.mockito.kotlin.doReturn

/**
 * Currently not used.
 */
class SpyPreferences(@ApplicationContext private val context: Context) : Preferences(context) {

    private val prefs: Preferences = spy(this)

    init {
        doReturn(Uri.parse("")).`when`(prefs).userDocumentUriOf(Mockito.anyString())
    }
}
