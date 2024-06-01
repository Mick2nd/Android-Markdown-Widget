package ch.tiim.markdown_widget

import android.app.Application
import android.util.Log
import ch.tiim.markdown_widget.di.AppComponent

private const val TAG = "Main"

/**
 * Custom [Application] instance
 */
class Main : Application() {
    private lateinit var appComponent: AppComponent
    // TEST CODE
    // @Inject @Named("GLOBAL-1") lateinit var observer: FileContentObserver

    /**
     * Override. Instantiates the [appComponent].
     */
    override fun onCreate() {
        super.onCreate()
        appComponent = AppComponent.create(this, applicationContext)
        // TEST CODE
        appComponent.inject(this)
        Log.i(TAG, "Application Main created")
    }

    /**
     * Never called on production
     */
    override fun onTerminate() {
        Log.i(TAG, "Application Main terminated")
        super.onTerminate()
    }
}
