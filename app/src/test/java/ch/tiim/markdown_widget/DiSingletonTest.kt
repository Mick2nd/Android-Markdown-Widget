package ch.tiim.markdown_widget

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import ch.tiim.markdown_widget.di.DaggerTestAppComponent
import ch.tiim.markdown_widget.di.TestAppComponent
import org.junit.After
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

/**
 * Test of the Singleton properties of components
 * - Factory of AppComponent delivers a new instance after each call
 * - preferences provides real Singleton
 */
@RunWith(MockitoJUnitRunner::class)
class DiSingletonTest {

    private lateinit var app: Application
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var contentResolver: ContentResolver
    private lateinit var appComponent: TestAppComponent
    private lateinit var factory: TestAppComponent.Factory
    private lateinit var type: String

    companion object {
        private lateinit var log: MockedStatic<Log>
        private lateinit var mockedUri: MockedStatic<Uri>
        private lateinit var uri: Uri
        private lateinit var documentsContract: MockedStatic<DocumentsContract>

        @JvmStatic
        @BeforeClass
        fun setupClass(): Unit {

            log = mockStatic(Log::class.java)
            mockedUri = mockStatic(Uri::class.java)
            uri = mock(Uri::class.java)
            documentsContract = mockStatic(DocumentsContract::class.java)
            `when` ( Uri.parse(anyString()) ).thenReturn(uri)
            `when` ( DocumentsContract.getTreeDocumentId(any()) ).thenReturn("")
        }
    }

    @Before
    fun setup() {

        app = mock(Application::class.java)
        context = mock(Context::class.java)
        type = ""
        sharedPreferences = mock(SharedPreferences::class.java)
        contentResolver = mock(ContentResolver::class.java)
        factory = DaggerTestAppComponent.factory()
        appComponent = factory.create(app, context, type)
    }

    @Test
    fun appComponentSingleton_Ok() {
        assertNotEquals(factory.create(app, context, type), factory.create(app, context, type))
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
     * It seems to be problematic to instantiate FileServices (fileChecker). A mocked provideUri
     * method did help.
     */
    @Test
    fun fileCheckerSingleton_Ok() {
        assertEquals(appComponent.fileChecker(), appComponent.fileChecker())
    }

    @After
    fun teardown() {
        log.reset()
    }
}
