package ch.tiim.markdown_widget

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.provider.DocumentsContract
import android.util.Log
import ch.tiim.markdown_widget.di.AppComponent
import ch.tiim.markdown_widget.di.DaggerAppComponent
import org.junit.After
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.FileNotFoundException

/**
 * Test of the Singleton properties of components
 * - Factory of AppComponent delivers a new instance after each call
 * - preferences provides real Singleton
 */
@RunWith(MockitoJUnitRunner::class)
class DiSingletonTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var contentResolver: ContentResolver
    private lateinit var appComponent: AppComponent
    private lateinit var factory: AppComponent.Factory
    private lateinit var type: String

    companion object {
        private lateinit var log: MockedStatic<Log>
        private lateinit var documentsContract: MockedStatic<DocumentsContract>

        @JvmStatic
        @BeforeClass
        fun setupClass(): Unit {
            log = Mockito.mockStatic(Log::class.java)
            documentsContract = mockStatic(DocumentsContract::class.java)
            `when` ( DocumentsContract.getTreeDocumentId(any()) ).thenReturn("")
        }
    }

    @Before
    fun setup() {

        context = mock(Context::class.java)
        type = ""
        sharedPreferences = mock(SharedPreferences::class.java)
        contentResolver = mock(ContentResolver::class.java)
        /* BELOW 'STUBS' are needed for fileChecker only, fileChecker does not work anyway.
        `when` ( context.getSharedPreferences(anyString(), anyInt()) ).thenReturn(sharedPreferences)
        `when` ( context.contentResolver ).thenReturn(contentResolver)
        `when` ( sharedPreferences.getString(anyString(), anyString()) ).thenReturn("")
        `when` ( contentResolver.query(any(), any(), any(), any()) ).thenReturn(null)
         */
        appComponent = AppComponent.create(context, type)
        factory = DaggerAppComponent.factory()
    }

    @Test
    fun appComponentSingleton_Ok() {
        assertNotEquals(factory.create(context, type), factory.create(context, type))
    }

    @Test
    fun preferencesSingleton_Ok() {
        assertEquals(appComponent.preferences(), appComponent.preferences())
    }

    @Test
    fun externalStoragePathHandlerSingleton_Ok() {
        assertEquals(appComponent.externalStoragePathHandler(), appComponent.externalStoragePathHandler())
    }

    @Test
    fun storagePermissionCheckerSingleton_Ok() {
        assertEquals(appComponent.storagePermissionChecker(), appComponent.storagePermissionChecker())
    }

    /**
     * It seems to be problematic to instantiate FileServices (fileChecker).
     * Reason is the Exception thrown on the provideUri3 request. The mocking trial below does ot
     * help.
     */
    @Ignore("Problem detected")
    @Test
    fun fileCheckerSingleton_Ok() {
        // val prefs = mock(Preferences::class.java)
        // `when` ( prefs.userDocumentUriOf(anyString()) ).thenReturn(null)
        assertEquals(appComponent.fileChecker(), appComponent.fileChecker())
    }

    @After
    fun teardown() {
        log.reset()
    }
}
