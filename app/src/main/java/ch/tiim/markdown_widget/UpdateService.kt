package ch.tiim.markdown_widget

import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.FileObserver
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.util.keyIterator
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import java.io.File

private const val TAG = "UpdateService"

/**
 * The intent is to have a service, e.g. a long living piece of code
 * This piece of code hosts an Observer for styles file changes
 * Each change triggers a Markdown File Widget update
 */
class UpdateService : Service() {
    var stylesObserver: StylesObserver? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate() {
        super.onCreate()
        stylesObserver = StylesObserver(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // return super.onStartCommand(intent, flags, startId)
        Log.d(TAG,"STARTING")

        // If we get killed, after returning from here, restart
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // super.onDestroy()
        Log.d(TAG,"DESTROY")
    }

    override fun onBind(intent: Intent): IBinder {
        Log.w(TAG, "onBind not implemented yet")
        TODO("Return the communication channel to the service.")
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
class StylesObserver(val context: Context) :
    FileObserver(File(context.filesDir, "public/userstyle.css"), FileObserver.MODIFY or FileObserver.CREATE) {

    init {
        startWatching()
        Log.d(TAG, "StylesObserver started")
    }

    override fun onEvent(event: Int, path: String?) {
        Log.d(TAG, "UserStyle.css updated")
        sendUpdateRequest(context)
    }

    private fun sendUpdateRequest(context: Context) {
        for (appWidgetId in MarkdownFileWidget.cachedMarkdown.keyIterator()) {
            val pendingIntent = getUpdatePendingIntent(context, appWidgetId)
            pendingIntent.send()
        }
    }
}

class UpdateViewModel (
    application: Application
): AndroidViewModel(application) {
    fun startService(){
        val context:Application = getApplication()
        context.startService(Intent(context, UpdateService::class.java))

    }

    override fun onCleared() {
        super.onCleared()
        val context:Application = getApplication()
        context.stopService(Intent(context, UpdateService::class.java))
        Log.d("NetworkMonitorViewModel","cleared")
    }
}

/*
class HomeFragment : Fragment() {
    private val updateViewModel: UpdateViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        updateViewModel.startService()
        // rest of the code not shown here
        return null
    }

    override fun onResume() {
        super.onResume()
        updateViewModel.startService()
    }
}
*/