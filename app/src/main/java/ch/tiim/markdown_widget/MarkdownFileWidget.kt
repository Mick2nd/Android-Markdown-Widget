package ch.tiim.markdown_widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.SparseArray
import android.widget.RemoteViews
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import java.lang.reflect.InvocationTargetException
import javax.inject.Inject

private const val TAG = "MarkdownFileWidget"

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [MarkdownFileWidgetConfigureActivity]
 */
@AndroidEntryPoint
class MarkdownFileWidget : AppWidgetProvider() {
    companion object {
        val cachedMarkdown: SparseArray<MarkdownRenderer> = SparseArray()
    }

    /**
     * Preferences injected in this central location.
     */
    @Inject lateinit var prefs: Preferences
    @Inject lateinit var contentCache: ContentCache

    /**
     * Init block. Performs the injection.
     */
    init {
        Log.i(TAG, "Init block")
    }

    /**
     * Updates all requested Widgets
     *
     * @param context the [Context] instance
     * @param appWidgetIds array of requested widgets
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.i(TAG, "onUpdate invoked on ${context.applicationContext} for ${appWidgetIds.toList()}")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * Handles changed options, but especially View dimension changes
     *
     * @param context the [Context] instance
     * @param appWidgetManager the [AppWidgetManager]
     * @param appWidgetId id of the Widget to be changed
     * @param newOptions
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        Log.i(TAG, "onAppWidgetOptionsChanged invoked on ${context.applicationContext} for $appWidgetId")
        if (appWidgetManager != null) {
            loadRenderer(context, appWidgetId, true) {
                loadRendererCb(context, appWidgetManager, appWidgetId)
                Log.d(TAG, "Update after Options Changed")
            }
        }
    }

    /**
     * When the user deletes the widget, delete the preference associated with it.
     *
     * @param context the [Context] instance
     * @param appWidgetIds array of app widgets
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.i(TAG, "onDeleted invoked for ${appWidgetIds.toList()}")
        for (appWidgetId in appWidgetIds) {
            prefs.delete(appWidgetId)
            cachedMarkdown.delete(appWidgetId)
        }
    }

    /**
     * Enter relevant functionality for when the first widget is created.
     *
     * @param context the [Context] instance
     *
     */
    override fun onEnabled(context: Context) {
        Log.i(TAG, "onEnabled")
    }

    /**
     * Enter relevant functionality for when the last widget is disabled.
     *
     * @param context the [Context] instance
     */
    override fun onDisabled(context: Context) {
        Log.i(TAG, "onDisabled")
    }

    /**
     * THIS OVERRIDE IS NORMALLY NOT REQUIRED. BASE CLASS IMPLEMENTATION IS SUFFICIENT.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            Log.d(TAG, "onReceive ${intent!!.action!!}")
            super.onReceive(context, intent)
        } catch (err: Throwable) {
            Log.e(TAG, "Unexpected exception in onReceive: $err")
        }
    }

    /**
     * Updates a single widget.
     *
     * @param context the [Context] instance
     * @param appWidgetManager the [AppWidgetManager]
     * @param appWidgetId the integer id of the app widget
     */
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        loadRenderer(context, appWidgetId, true) {
            loadRendererCb(context, appWidgetManager, appWidgetId)
            Log.d(TAG, "Update after Update Request")

            registerPendingIntents(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * Either instantiates a Renderer instance or extracts it from Cache.
     * In either case the content of the Widget is drawn.
     * Finally the given callback is invoked.
     *
     * @param appWidgetId the Widget
     * @param checkForChange defines the relevance of the *needsUpdate* call
     * @param cb the callback to be invoked
     */
    private fun loadRenderer(context: Context, appWidgetId: Int, checkForChange: Boolean, cb: (()->Unit)) {
        val fileUri = prefs.markdownUriOf(appWidgetId)
        if (fileUri.scheme == null) {
            Log.i(TAG, "Invoked Markdown Widget with empty Uri, probably first time call.")
            return
        }
        var s = ""
        try {
            s = contentCache[fileUri]                       // throws
        } catch(err: Throwable) {
            val msg = "Could not load md file: $err"
            Log.e(TAG, msg)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }

        val widgetSizeProvider = WidgetSizeProvider(context)
        val widthRatio = widgetSizeProvider.getWidgetWidthRatio(appWidgetId, prefs)
        var md = cachedMarkdown[appWidgetId]
        if (md == null || (checkForChange && md.needsUpdate(s, widthRatio))) {
            md = MarkdownRenderer(context, s, widthRatio, cb)
            cachedMarkdown.put(appWidgetId, md)
            Log.d(TAG, "New Renderer instance created ${md} from $fileUri: $s")
        } else {
            md.refresh(cb)
            // NOT RELIABLE? - SEEMS TO BE RELIABLE AND FASTER
            // cb()
            Log.d(TAG, "Cached Renderer instance used ${md} from $fileUri: $s")
        }
    }

    /**
     * Callback used by [loadRenderer]
     */
    private fun loadRendererCb(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val md = cachedMarkdown[appWidgetId]
        if (md == null) {
            Log.e(TAG, "NO RENDERER TO UPDATE")
            return
        }

        val size = WidgetSizeProvider(context)
        val (width, height) = size.getWidgetsSize(appWidgetId)
        val views = RemoteViews(context.packageName, R.layout.markdown_file_widget)

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val bitmap = md.getBitmap(width, height)
            views.setImageViewBitmap(R.id.renderImg, bitmap)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun registerPendingIntents(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.markdown_file_widget)
        views.setOnClickPendingIntent(
            R.id.renderImg,
            getIntent(context, appWidgetId)
        )
        views.setOnClickPendingIntent(
            R.id.refreshImageButton,
            getUpdatePendingIntent(context, appWidgetId)
        )
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    /**
     * Returns the Intent to be used to request the invocation of the Markdown editor.
     * Depends on the configured method during Widget creation.
     * TODO: obsidian not reacting
     *
     * @param appWidgetId the id of the App Widget initiating this request
     * @return pending intent
     */
    private fun getIntent(context: Context, appWidgetId: Int): PendingIntent? {

        val uri = prefs.markdownUriOf(appWidgetId)
        val tapBehavior = prefs[appWidgetId, PREF_BEHAVIOUR, TAP_BEHAVIOUR_DEFAULT_APP]

        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val pendingFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val intent = Intent(Intent.ACTION_EDIT).apply {
            this.flags = flags
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

            when (tapBehavior) {
                TAP_BEHAVIOUR_DEFAULT_APP -> setDataAndType(uri.normalizeScheme(), "text/plain")
                TAP_BEHAVIOUR_OBSIDIAN -> data = uri.toObsidian(context)
                TAP_BEHAVIOUR_ITSELF -> {
                    setComponent(ComponentName(context.packageName, MainActivity::class.qualifiedName!!))
                    setDataAndType(uri.normalizeScheme(), "text/plain")
                }
                else -> { return null }
            }
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingFlags)
        Log.d(TAG, "Intent assembled: ${intent.resolveActivity(context.packageManager)}, data : ${intent.data}, extras: ${intent.extras} , tap: $tapBehavior")
        return pendingIntent
    }
}

/**
 * Can be used to Broadcast update requests to the own Widget.
 *
 * @param context the [Context] instance
 * @param appWidgetId the integer id of the app widget
 * @return intent of type [PendingIntent]
 */
fun getUpdatePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
    val intentUpdate = Intent(context, MarkdownFileWidget::class.java)
    intentUpdate.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    val idArray = intArrayOf(appWidgetId)
    intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idArray)
    val pendingUpdate = PendingIntent.getBroadcast(
        context,
        appWidgetId,
        intentUpdate,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    Log.d(TAG, "Intent assembled: ${intentUpdate.resolveActivity(context.packageManager)}, data : ${intentUpdate.data}, extras: ${intentUpdate.extras}")
    return pendingUpdate
}

/**
 * Extracts Dimensions of Widget in pixels. As a recherche in the Internet yielded, multiplication
 * by *density* transforms dp into px,
 * @see <a href="https://stackoverflow.com/questions/76087269/why-displaymetrics-density-is-wrong">
 *     Stackoverflow</a> .
 */
class WidgetSizeProvider(
    private val context: Context // Do not pass Application context
) {
    /**
     * Returns Dimensions of Widget as stored in the options.
     *
     * @param widgetId the id of the widget
     * @return Dimensions in Pixels
     */
    fun getWidgetsSize(widgetId: Int): Pair<Int, Int> {
        val manager = AppWidgetManager.getInstance(context)
        val isPortrait = context.resources.configuration.orientation == ORIENTATION_PORTRAIT
        val (width, height) = listOf(
            if (isPortrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
            else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
            if (isPortrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
            else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
        ).map { manager.getAppWidgetOptions(widgetId).getInt(it, 100).dp.toInt() }

        Log.d(TAG, "Device size: $width $height")
        return width to height
    }

    /**
     * This method calculates the ratio between widget width and screen width. This is used to
     * perform widget updates in the index.html javascript.
     */
    fun getWidgetWidthRatio(widgetId: Int, prefs: Preferences) : Float {
        val manager = AppWidgetManager.getInstance(context)
        val isPortrait = context.resources.configuration.orientation == ORIENTATION_PORTRAIT
        val widthSetting =
            if (isPortrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
            else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH

        val width = manager.getAppWidgetOptions(widgetId).getInt(widthSetting, -1).toFloat()
        val screenWidth = prefs[SCREEN_WIDTH, "1000"].toFloat()
        val result = (if (width > 0) width / screenWidth  else 0.9f).dp
        Log.d(TAG, "Calculated width ratio: $result, Screen width: $screenWidth")
        return result
    }

    private val Number.dp: Float get() = this.toFloat() * Resources.getSystem().displayMetrics.density
}