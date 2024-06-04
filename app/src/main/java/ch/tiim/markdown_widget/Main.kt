package ch.tiim.markdown_widget

import android.app.Application
import android.util.Log
import ch.tiim.markdown_widget.di.CustomEntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.android.HiltAndroidApp

private const val TAG = "Main"

/**
 * Custom [Application] instance
 */
@HiltAndroidApp
class Main : Application() {

    /**
     * Override. Instantiates the [appComponent].
     */
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application Main created")
        Log.i(TAG, "Application Main created: ${EntryPoints.get(applicationContext, CustomEntryPoint::class.java)}")
    }

    /**
     * Never called on production
     */
    override fun onTerminate() {
        Log.i(TAG, "Application Main terminated")
        super.onTerminate()
    }
}
