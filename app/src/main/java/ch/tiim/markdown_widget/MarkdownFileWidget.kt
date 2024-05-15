package ch.tiim.markdown_widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import android.util.SparseArray
import android.widget.RemoteViews
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader


private const val TAG = "MarkdownFileWidget"

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [MarkdownFileWidgetConfigureActivity]
 */
class MarkdownFileWidget : AppWidgetProvider() {
    companion object {
        val cachedMarkdown: SparseArray<MarkdownRenderer> = SparseArray()
    }

    /**
     * Updates all requested Widgets
     * @param appWidgetIds array of requested widgets
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        Log.i(TAG, "onUpdate")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * Handles changed options, but especially View dimension changes
     * @param appWidgetId id of the Widget to be changed
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        Log.i(TAG, "onAppWidgetOptionsChanged: ${context.applicationContext}")
        if (appWidgetManager != null) {
            loadRenderer(context, appWidgetId, false) {
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
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.i(TAG, "onDeleted")
        for (appWidgetId in appWidgetIds) {
            deletePrefs(context, appWidgetId)
            cachedMarkdown.delete(appWidgetId)
        }
    }

    /**
     * Enter relevant functionality for when the first widget is created
     */
    override fun onEnabled(context: Context) {
        Log.i(TAG, "onEnabled")
    }

    /**
     * Enter relevant functionality for when the last widget is disabled
     */
    override fun onDisabled(context: Context) {
        Log.i(TAG, "onDisabled")
    }

    /**
     * THIS OVERRIDE IS NORMALLY NOT REQUIRED
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive ${intent!!.action!!}")
        super.onReceive(context, intent)
    }

    /**
     * Updates a single widget
     */
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        /*
        val pendingUpdate = getUpdatePendingIntent(context, appWidgetId)
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            try {
                pendingUpdate.send()
                Log.d(TAG, "Sent update request")
            } catch (e: CanceledException) {
                e.printStackTrace()
                Log.e(TAG, e.toString())
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
        */

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
            val fileUri = Uri.parse(loadPref(context, appWidgetId, PREF_FILE, ""))
            val tapBehavior = loadPref(context, appWidgetId, PREF_BEHAVIOUR, TAP_BEHAVIOUR_DEFAULT_APP)
            if (tapBehavior != TAP_BEHAVIOUR_NONE) {
                views.setOnClickPendingIntent(
                    R.id.renderImg,
                    getIntent(context, fileUri, tapBehavior, context.contentResolver)
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
     * Either instantiates a Renderer instance or extracts it from Cache
     * In either case the content of the Widget is drawn
     * Finally the given callback is invoked
     * @param appWidgetId the Widget
     * @param checkForChange defines the relevance of the *needsUpdate* call
     * @param cb the callback to be invoked
     */
    private fun loadRenderer(context: Context, appWidgetId: Int, checkForChange: Boolean, cb: (()->Unit)) {
        val fileUri = Uri.parse(loadPref(context, appWidgetId, PREF_FILE, ""))
        val s = loadMarkdown(context, fileUri)
        var md = cachedMarkdown[appWidgetId]
        if (md == null || (checkForChange && md.needsUpdate(s))) {
            val widgetSizeProvider = WidgetSizeProvider(context)
            val (width, height) = widgetSizeProvider.getScreenSize()
            md = MarkdownRenderer(context, width / 2, height / 2, s, cb)
            cachedMarkdown.put(appWidgetId, md)
            Log.d(TAG, "New Renderer instance created ${md}")
        } else {
            // NOT RELIABLE
            // cb()
            md.refresh(cb)
            Log.d(TAG, "Cached Renderer instance used ${md}")
        }
    }
}

/**
 * Can be used to Broadcast update requests to the own Widget
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
 * Returns the Intent to be used to request the invocation of the Markdown editor
 * Depends on the configured method during Widget creation
 * @param uri Uri of the file
 * @param tapBehavior configured Tap method
 * @return pending intent
 */
fun getIntent(context: Context, uri: Uri, tapBehavior: String, c: ContentResolver): PendingIntent {
    val intent = Intent(Intent.ACTION_EDIT)
    if (tapBehavior == TAP_BEHAVIOUR_DEFAULT_APP) {
        intent.setDataAndType(uri.normalizeScheme(), "text/plain")
        //intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    } else if (tapBehavior == TAP_BEHAVIOUR_OBSIDIAN) {
        intent.data = Uri.parse("obsidian://open?file=" + Uri.encode(getFileName(uri, c)))
    }
    return PendingIntent.getActivity(context, 555, intent, PendingIntent.FLAG_IMMUTABLE)
}

/**
 * Uri to File name converter
 */
fun getFileName(uri: Uri, c: ContentResolver): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor: Cursor? = c.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val i = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                result = cursor.getString(i)
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result!!.lastIndexOf('/')
        if (cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result
}

/**
 * Loads the Markdown given the Uri of a file
 * @param uri the Uri of a file
 * @return the string containing the Markdown
 */
fun loadMarkdown(context: Context, uri: Uri): String {
    try {
        Log.i(TAG, uri.toString())
        val ins: InputStream = context.contentResolver.openInputStream(uri)!!
        val reader = BufferedReader(InputStreamReader(ins, "utf-8"))
        val data = reader.lines().reduce { s, t -> s + "\n" + t }
        reader.close()
        ins.close()
        return data.get()
    } catch (err: FileNotFoundException) {
        return err.toString()
    } catch (err: Exception) {
        return err.toString()
    } finally {
    }
}

/**
 * Extracts Dimensions of Widget or Screen in Density Points
 */
class WidgetSizeProvider(
    private val context: Context // Do not pass Application context
) {
    /**
     * Returns Dimensions of Widget as stored in the options
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
     * Returns the Dimensions of the Screen
     * @return Dimensions in Density Points
     */
    fun getScreenSize() : Pair<Int, Int> {
        val displayMetrics = Resources.getSystem().displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        return width.dp.toInt() to height.dp.toInt()
    }

    private val Number.dp: Float get() = this.toFloat() * Resources.getSystem().displayMetrics.density
}