package ch.tiim.markdown_widget

import android.app.Application
import android.os.Environment
import android.util.Log
import ch.tiim.markdown_widget.di.AppComponent

private const val TAG = "Main"

class Main : Application() {
    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = AppComponent.create(applicationContext, Environment.DIRECTORY_DOCUMENTS)
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
