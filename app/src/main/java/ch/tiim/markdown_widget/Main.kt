package ch.tiim.markdown_widget

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

private const val TAG = "Main"

/**
 * Custom [Application] instance.
 */
@HiltAndroidApp
class Main : Application() {

    /**
     * Override. Instantiates the [Application].
     */
    override fun onCreate() {
        super.onCreate()
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
