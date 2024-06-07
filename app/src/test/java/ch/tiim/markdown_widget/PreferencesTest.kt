package ch.tiim.markdown_widget

import android.content.Context
import ch.tiim.markdown_widget.fakes.FakePreferences
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner


@RunWith(MockitoJUnitRunner::class)
class PreferencesTest {

    private lateinit var context: Context
    private lateinit var prefs: FakePreferences

    @Before
    fun setup() {
        context = Mockito.mock(Context::class.java)
        prefs = FakePreferences(context)
    }

    @Test
    fun filePreferenceForAppId_1_Ok() {
        assertEquals("x123", prefs["1--$PREF_FILE", ""])
        assertEquals("x123", prefs[1, PREF_FILE, ""])
    }

    @Test
    fun filePreferenceForAppId_2_providesDefault() {
        val default1 = "~xyz~"
        val default2 = "^xyz^"
        assertEquals(default1, prefs["2--$PREF_FILE", default1])
        assertEquals(default2, prefs[2, PREF_FILE, default2])
    }

    @Test
    fun unusedPreference_providesDefault() {
        val unusedPreference = "unused"
        val default = "~xyz~"
        assertEquals(default, prefs[unusedPreference, default])
    }

    @Test
    fun filePreferenceForAppId_2_providesSetValue() {
        val default = "~xyz~"
        val value = "y234"
        val pref = "2--$PREF_FILE"
        prefs[2, PREF_FILE] = value
        assertEquals(value, prefs[pref, default])
        assertEquals(value, prefs[2, PREF_FILE, default])
    }

    @After
    fun teardown() {

    }
}
