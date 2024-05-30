package ch.tiim.markdown_widget

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doAnswer


@RunWith(MockitoJUnitRunner::class)
class PreferencesTest2 {

    @Mock private lateinit var context: Context
    @Mock private lateinit var editor: Editor
    @Mock private lateinit var sharedPreferences: SharedPreferences
    private val hashMap = HashMap<String, String>()
    private lateinit var prefs: Preferences

    @Before
    fun setup() {
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        `when`(sharedPreferences.edit()).thenReturn(editor)
        `when`(sharedPreferences.getString(anyString(), anyString())).thenAnswer {
            answer ->
                val key = answer.arguments[0] as String
                val default = answer.arguments[1] as String
                if (key in hashMap) hashMap[key] else default
        }
        `when`(editor.putString(anyString(), anyString())).doAnswer {
            answer ->
                val key = answer.arguments[0] as String
                val value = answer.arguments[1] as String
                hashMap[key] = value
                null
        }
        `when`(editor.remove(anyString())).doAnswer {
            answer ->
                val key = answer.arguments[0] as String
                hashMap.remove(key)
                null
        }
        prefs = Preferences(context)
    }

    @Test
    fun unusedPreference_providesDefault() {
        val unusedPreference = "unused"
        val default = "~xyz~"
        Assert.assertEquals(default, prefs[unusedPreference, default])
    }

    @Test
    fun setPreference_providesSetValue() {
        val default = "~xyz~"
        val value = "y234"
        val pref = "to_be_used"
        prefs[pref] = value
        Assert.assertEquals(value, prefs[pref, default])
    }

    @Test
    fun removedPreference_providesDefault() {
        val default = "~xyz~"
        val value = "y234"
        val pref = "to_be_used"
        prefs[pref] = value
        prefs.delete(pref)
        Assert.assertEquals(default, prefs[pref, default])
    }

    @Test
    fun clearCache_retainSetting() {
        val default = "~xyz~"
        val value = "y234"
        val pref = "to_be_used"
        prefs[pref] = value
        prefs.clearCache()
        Assert.assertEquals(value, prefs[pref, default])
    }
}
