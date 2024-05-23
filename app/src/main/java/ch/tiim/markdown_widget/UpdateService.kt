package ch.tiim.markdown_widget

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.util.keyIterator
import androidx.lifecycle.AndroidViewModel
import ch.tiim.markdown_widget.di.AppComponent

private const val TAG = "UpdateService"
private const val NOTIFICATION = 1

/**
 * The intent is to have a service, e.g. a long living piece of code.
 * This piece of code hosts an Observer for styles file changes.
 * Each change triggers a Markdown File Widget update.
 * TODO:
 * - support versions before Q
 * - make observed file(s) path configurable
 */
class UpdateService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private var stylesObserver: FileObserver? = null

    /**
     * Creates the service
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "The service has been created".uppercase())
        val notification = createNotification()
        startForeground(NOTIFICATION, notification)
        stylesObserver = createStylesObserver(applicationContext)
    }

    /**
     * Starts / Stops the service
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG,"onStartCommand executed with startId: $startId")
        if (intent != null) {
            val action = intent.action
            Log.d(TAG,"Using an intent with action $action")
            when (action) {
                "START" -> startService()
                "STOP"  -> stopService()
                else -> Log.w(TAG,"This should never happen. No known action in the received intent")
            }
        } else {
            Log.w(TAG, "With a null intent. It has been probably restarted by the system.")
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    /**
     * Handles the onDestroy event
     */
    override fun onDestroy() {
        stylesObserver = null
        Log.d(TAG, "The service has been destroyed".uppercase())
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
        super.onDestroy()
    }

    /**
     * Handles the onBind event
     */
    override fun onBind(intent: Intent): IBinder? {
        Log.w(TAG, "onBind not implemented yet")
        return null
    }

    /**
     * Performs the START command. Besides notifying the user does nothing.
     */
    private fun startService() {
        if (isServiceStarted)
            return

        isServiceStarted = true
        Toast.makeText(this, "Starting the foreground service", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Starting the foreground service task")
    }

    /**
     * Performs the STOP command
     */
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
            Log.w(TAG, "Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
    }

    /**
     * Code from Web. Creates a notification channel for the foreground service.
     */
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
 * Does the real work: watching for file changes of the userstyle.css file and performing widget updates.
 */
fun createStylesObserver(context: Context) : FileObserver {
    fun getUserStyle(): Uri {
        return AppComponent.instance.preferences()["userstyle.css"]
    }

    fun sendUpdateRequest(context: Context) {
        Log.i(TAG, "UserStyle.css updated")
        for (appWidgetId in MarkdownFileWidget.cachedMarkdown.keyIterator()) {
            val pendingIntent = getUpdatePendingIntent(context, appWidgetId)
            pendingIntent.send()
        }
    }

    val o =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            object : FileObserver(getUserStyle().toFile(), MODIFY or CREATE) {
                override fun onEvent(event: Int, path: String?) {
                    sendUpdateRequest(context)
                }
            } else object : FileObserver(getUserStyle().toString(), MODIFY or CREATE) {
                override fun onEvent(event: Int, path: String?) {
                    sendUpdateRequest(context)
                }
            }
    return o
}

/**
 * A class to start and stop the service [UpdateService]
 */
class UpdateViewModel (
    application: Application
) : AndroidViewModel(application) {

    /**
     * Start the service
     */
    fun startService(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d(TAG, "About to start Update Service")
            val context: Application = getApplication()
            val intent = Intent(context, UpdateService::class.java)
            intent.action = "START"
            context.startForegroundService(intent)
        }
    }

    /**
     * Handles [onCleared] to stop the service
     */
    override fun onCleared() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val context: Application = getApplication()
            val intent = Intent(context, UpdateService::class.java)
            intent.action = "STOP"
            context.stopService(intent)
            Log.d(TAG, "Cleared")
            super.onCleared()
        }
    }
}
