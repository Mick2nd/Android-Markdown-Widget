package ch.tiim.markdown_widget

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.FileObserver
import android.os.IBinder
import android.os.PowerManager
import android.telephony.ServiceState
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat.startForeground
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

    private val NOTIFICATION = 1
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private var stylesObserver: StylesObserver? = null

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "The service has been created".uppercase())
        val notification = createNotification()
        startForeground(NOTIFICATION, notification)                 //, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        stylesObserver = StylesObserver(applicationContext)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG,"onStartCommand executed with startId: $startId")
        if (intent != null) {
            val action = intent.action
            Log.d(TAG,"Using an intent with action $action")
            when (action) {
                "START" -> startService()
                "STOP"  -> stopService()
                else -> Log.d(TAG,"This should never happen. No action in the received intent")
            }
        } else {
            Log.d(TAG, "With a null intent. It has been probably restarted by the system.")
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onDestroy() {
        stylesObserver = null
        Log.d(TAG, "The service has been destroyed".uppercase())
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.w(TAG, "onBind not implemented yet")
        return null
    }

    /**
     * Performs the START command
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startService() {
        if (isServiceStarted)
            return

        isServiceStarted = true
        Toast.makeText(this, "Service starting its task - 1", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Starting the foreground service task")
    }

    private fun stopService() {
        Log.d(TAG, "Stopping the foreground service")
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.d(TAG, "Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
    }

    private fun createNotification() : Notification {
        val notificationChannelId = "ENDLESS SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                "Endless Service notifications channel",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Endless Service channel"
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }
        // val pendingIntent = getUpdatePendingIntent(this, 5)

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
            this,
            notificationChannelId
        ) else Notification.Builder(this)

        return builder
            .setContentTitle("Endless Service")
            .setContentText("This is your favorite endless service working")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Ticker text")
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()
    }
}

/**
 * Does the real work: watching for for file changes and performing widget updates
 */
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
) : AndroidViewModel(application) {

    @RequiresApi(Build.VERSION_CODES.O)
    fun startService(){
        Log.d(TAG, "About to start Update Service")
        val context:Application = getApplication()
        val intent = Intent(context, UpdateService::class.java)
        intent.action = "START"
        context.startForegroundService(intent)
    }

    override fun onCleared() {
        val context:Application = getApplication()
        val intent = Intent(context, UpdateService::class.java)
        intent.action = "STOP"
        context.stopService(intent)
        Log.d(TAG,"Cleared")
        super.onCleared()
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