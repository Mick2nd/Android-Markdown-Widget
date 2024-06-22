package ch.tiim.markdown_widget

import android.net.Uri
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.After
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject
import javax.inject.Named

/**
 * Test of the Singleton properties of components.
 * - preferences provides real Singleton
 */
@HiltAndroidTest
@Config(application = HiltTestApplication::class, manifest=Config.NONE)
@RunWith(RobolectricTestRunner::class)
class DiSingletonTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var prefs1: Preferences
    @Inject lateinit var prefs2: Preferences
    @Inject lateinit var pathHandler1: ExternalStoragePathHandler
    @Inject lateinit var pathHandler2: ExternalStoragePathHandler
    @Inject lateinit var permissionChecker1: StoragePermissionChecker
    @Inject lateinit var permissionChecker2: StoragePermissionChecker
    @Inject lateinit var changeObserver1: ChangeObserver
    @Inject lateinit var changeObserver2: ChangeObserver
    @Inject @Named("GLOBAL") lateinit var uri1: Uri
    @Inject @Named("GLOBAL") lateinit var uri2: Uri

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        hiltRule.inject()
    }

    @Test
    fun preferencesSingleton_Ok() {
        assertEquals(prefs1, prefs2)
    }

    @Test
    fun externalStoragePathHandlerSingleton_Ok() {
        assertEquals(pathHandler1, pathHandler2)
    }

    @Test
    fun storagePermissionCheckerSingleton_Ok() {
        assertEquals(permissionChecker1, permissionChecker2)
    }

    /**
     * It seems to be problematic to instantiate FileServices (fileChecker). A mocked provideUri
     * method did help.
     */
    @Test
    fun fileCheckerSingleton_Ok() {
        assertEquals(changeObserver1, changeObserver2)
    }

    @Test
    fun uriNotSingleton_Ok() {
        assertNotSame(uri1, uri2)
        assertEquals(uri1, uri2)
    }

    @After
    fun teardown() {
        // log.reset()
    }
}
