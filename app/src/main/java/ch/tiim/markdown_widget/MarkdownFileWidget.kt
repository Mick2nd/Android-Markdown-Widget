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
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.SparseArray
import android.view.WindowManager
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
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
        val appWidgetIds: IntArray
            get() {
                return manager?.getAppWidgetIds(ComponentName("ch.tiim.markdown_widget", "MarkdownFileWidget")) ?: intArrayOf()
            }
        private var manager: AppWidgetManager? = null
    }

    /**
     * Preferences injected in this central location.
     */
    @Inject lateinit var prefs: Preferences

    /**
     * Init block. Performs the injection.
     */
    init {
        Log.d(TAG, "Here in init")
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
        Log.i(TAG, "onUpdate")
        manager = appWidgetManager
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
        Log.i(TAG, "onAppWidgetOptionsChanged: ${context.applicationContext}")
        appWidgetManager?.let { manager = appWidgetManager }
        if (appWidgetManager != null) {
            loadRenderer(context, appWidgetId, true) {
                val md = cachedMarkdown[appWidgetId]
                if (md == null) {
                    Log.e(TAG, "NO RENDERER TO UPDATE")
                    return@loadRenderer
                }
                val size = WidgetSizeProvider(context)
                val (width, height) = size.getWidgetsSize(appWidgetId)
                val views = RemoteViews(context.packageName, R.layout.markdown_file_widget)
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    val bitmap = md.getBitmap(width, height)
                    views.setImageViewBitmap(R.id.renderImg, bitmap)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    Log.d(TAG, "Update after Options Changed")
                }
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
        Log.i(TAG, "onDeleted")
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
        Log.d(TAG, "onReceive ${intent!!.action!!}")
        super.onReceive(context, intent)
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
            Log.d(TAG, "is ready!")
            val md = cachedMarkdown[appWidgetId]
            if (md == null) {
                Log.e(TAG, "NO RENDERER TO UPDATE")
                return@loadRenderer
            }
            val size = WidgetSizeProvider(context)
            val (width, height) = size.getWidgetsSize(appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.markdown_file_widget)
            val fileUri = Uri.parse(prefs[appWidgetId, PREF_FILE, ""])
            val tapBehavior = prefs[appWidgetId, PREF_BEHAVIOUR, TAP_BEHAVIOUR_DEFAULT_APP]
            if (tapBehavior != TAP_BEHAVIOUR_NONE) {
                views.setOnClickPendingIntent(
                    R.id.renderImg,
                    getIntent(context, fileUri, tapBehavior)
                )
            }

            val handler = Handler(Looper.getMainLooper())
            handler.post {
                val bitmap = md.getBitmap(width, height)
                views.setImageViewBitmap(R.id.renderImg, bitmap)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d(TAG, "Update after Update Request")
            }
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
        val s = FileServices(context, fileUri).content
        var md = cachedMarkdown[appWidgetId]
        if (md == null || (checkForChange && md.needsUpdate(s))) {
            val widgetSizeProvider = WidgetSizeProvider(context)
            val (width, height) = widgetSizeProvider.getScreenSize()
            md = MarkdownRenderer(context, width, height, s, cb)
            cachedMarkdown.put(appWidgetId, md)
            Log.d(TAG, "New Renderer instance created ${md}")
        } else {
            md.refresh(cb)
            // NOT RELIABLE? - SEEMS TO BE RELIABLE AND FASTER
            // cb()
            Log.d(TAG, "Cached Renderer instance used ${md}")
        }
    }
}

/**
 * Can be used to Broadcast update requests to the own Widget.
 *
 * @param context the [Context] instance
 * @param appWidgetId the integer id of the app widget
 * @return intent of type [PendingIntent]
 */
internal fun getUpdatePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
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
    return pendingUpdate
}

/**
 * Returns the Intent to be used to request the invocation of the Markdown editor.
 * Depends on the configured method during Widget creation.
 *
 * @param uri Uri of the file
 * @param tapBehavior configured Tap method
 * @return pending intent
 */
fun getIntent(context: Context, uri: Uri, tapBehavior: String): PendingIntent {
    val intent = Intent(Intent.ACTION_EDIT)
    if (tapBehavior == TAP_BEHAVIOUR_DEFAULT_APP) {
        intent.setDataAndType(uri.normalizeScheme(), "text/plain")
        //intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    } else if (tapBehavior == TAP_BEHAVIOUR_OBSIDIAN) {
        intent.data = uri.toObsidian(context)
    }
    return PendingIntent.getActivity(context, 555, intent, PendingIntent.FLAG_IMMUTABLE)
}

/**
 * Extracts Dimensions of Widget or Screen in Density Points
 */
class WidgetSizeProvider(
    private val context: Context // Do not pass Application context
) {
    /**
     * Returns Dimensions of Widget as stored in the options.
     *
     * @param widgetId the id of the widget
     * @return Dimensions in Density Points
     */
    fun getWidgetsSize(widgetId: Int): Pair<Int, Int> {
        val manager = AppWidgetManager.getInstance(context)
        val isPortrait = context.resources.configuration.orientation == ORIENTATION_PORTRAIT
        val (width, height) = listOf(
            if (isPortrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
            else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
            if (isPortrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
            else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
        ).map { manager.getAppWidgetOptions(widgetId).getInt(it, 0).dp.toInt() }

        Log.d(TAG, "Device size: $width $height")
        return width to height
    }

    /**
     * Returns the Dimensions of the Screen.
     *
     * @return Dimensions in Pixels
     */
    fun getScreenSize(manager: WindowManager? = null) : Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && manager != null) {
            val windowMetrics = manager.currentWindowMetrics
            val width = windowMetrics.bounds.width()
            val height = windowMetrics.bounds.height()

            return width to height
        } else {
            val displayMetrics = Resources.getSystem().displayMetrics
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels

            return width to height
        }
    }

    private val Number.dp: Float get() = this.toFloat() * Resources.getSystem().displayMetrics.density
}