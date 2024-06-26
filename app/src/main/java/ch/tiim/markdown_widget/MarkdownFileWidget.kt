package ch.tiim.markdown_widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.graphics.get
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

private const val TAG = "MarkdownFileWidget"
private const val STRIPE_HEIGHT = 100
private const val WIDGET_PADDING = 20
private const val WIDGET_HEIGHT = "widget_height"

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [MarkdownFileWidgetConfigureActivity].
 */
@AndroidEntryPoint
class MarkdownFileWidget : AppWidgetProvider() {
    companion object {
        val cachedMarkdown: SparseArray<MarkdownRenderer> = SparseArray()
    }

    /**
     * Preferences, ContentCache injected in this central location.
     */
    @Inject lateinit var prefs: Preferences
    @Inject lateinit var contentCache: ContentCache

    /**
     * Init block.
     */
    init {
        Log.i(TAG, "Init block")
    }

    /**
     * Updates all requested Widgets.
     *
     * @param context the [Context] instance
     * @param appWidgetManager the [AppWidgetManager]
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
     * Handles changed options, but especially View dimension changes.
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
        Log.i(TAG,"onAppWidgetOptionsChanged invoked on ${context.applicationContext} for $appWidgetId"
        )
        if (appWidgetManager != null) {
            registerPendingIntents(context, appWidgetManager, appWidgetId)
            loadRenderer(context, appWidgetId) {
                loadRendererCb(context, appWidgetId)
                Log.d(TAG, "Update after Options Changed")
            }
        }
    }

    /**
     * When the user deletes the widget, delete the preference associated with it.
     * TODO: handle the [ContentCache] ?
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
        registerPendingIntents(context, appWidgetManager, appWidgetId)
        cachedMarkdown.delete(appWidgetId)                                                          // force update
        loadRenderer(context, appWidgetId) {
            loadRendererCb(context, appWidgetId)
            Log.d(TAG, "Update after Update Request")
        }
    }

    /**
     * Either instantiates a Renderer instance or extracts it from Cache. In either case the content
     * of the Widget is drawn. Finally the given callback is invoked.
     * New: supply the Uri to the renderer instance as trial to load required embedded web content.
     *
     * @param appWidgetId the Widget
     * @param cb the callback to be invoked
     */
    private fun loadRenderer(context: Context, appWidgetId: Int, cb: (() -> Unit)) {
        val fileUri = prefs.markdownUriOf(appWidgetId)
        if (fileUri.scheme == null) {
            Log.i(TAG, "Invoked Markdown Widget with empty Uri, probably first time call.")
            return
        }
        var s = ""
        try {
            s = contentCache[fileUri]                       // throws
        } catch (err: Throwable) {
            val msg = "Could not load md file: $err"
            Log.e(TAG, msg)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }

        /*
         * The following code section tries to optimize the time used by the widget update.
         */
        val widgetSizeProvider = WidgetSizeProvider(context)
        val widthRatio = widgetSizeProvider.getWidgetWidthRatio(appWidgetId, prefs)
        var md = cachedMarkdown[appWidgetId]
        if (md == null || md.needsMarkdownUpdate(s)) {
            md = MarkdownRenderer(context, s, widthRatio, cb)
            cachedMarkdown.put(appWidgetId, md)
            Log.d(TAG, "Renderer created: $md for $fileUri")
        } else if (md.needsUpdate(widthRatio)) {
            md.refresh(widthRatio, cb)
            Log.d(TAG, "Renderer refreshed $md for $fileUri")
        } else {
            cb()
            Log.d(TAG, "Bitmap extracted from $md for $fileUri")
        }
    }

    /**
     * Callback used by [loadRenderer].
     */
    private fun loadRendererCb(
        context: Context,
        appWidgetId: Int
    ) {
        val md = cachedMarkdown[appWidgetId]
        if (md == null) {
            Log.e(TAG, "NO RENDERER TO UPDATE")
            return
        }

        val size = WidgetSizeProvider(context)
        val (width, height) = size.getWidgetsSize(appWidgetId)
        val bitmap = md.getBitmap(width, height)
        val bitmapMeta = bitmap.toBundleMeta()
        bitmapMeta.putInt(WIDGET_HEIGHT, height)
        registerRemoteAdapter(context, appWidgetId, bitmapMeta)
    }

    /**
     * Registers 1 [PendingIntent] for use by widget clicks, namely Update Broadcast event.
     */
    private fun registerPendingIntents(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.markdown_file_widget)
        views.setOnClickPendingIntent(
            R.id.refreshImageButton,
            getUpdatePendingIntent(context, appWidgetId)
        )
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    /**
     * Registers the Remote Adapter, responsible for display of ListView contents.
     *
     * @param bitmapMeta the [Bundle], representing the rendered markdown, to be displayed.
     */
    private fun registerRemoteAdapter(context: Context, appWidgetId: Int, bitmapMeta: Bundle) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val intent = Intent(context, MarkdownWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtras(bitmapMeta)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }

        val remoteViews = RemoteViews(context.packageName, R.layout.markdown_file_widget).apply {
            setRemoteAdapter(R.id.renderImg, intent)
            setEmptyView(R.id.renderImg, R.id.emptyView)

            setPendingIntentTemplate(
                R.id.renderImg,
                prefs.getEditPendingIntent(context, appWidgetId)
            )
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

    /**
     * Returns an Array of living appWidgetIds of this widget.
     */
    private fun getAppWidgetIds(context: Context) : IntArray {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, MarkdownFileWidget::class.java)
        return appWidgetManager.getAppWidgetIds(componentName)
    }
}

/**
 * The service responsible for the display.
 */
@AndroidEntryPoint
class MarkdownWidgetService : RemoteViewsService() {
    @Inject lateinit var prefs: Preferences

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        Log.i(TAG, "MarkdownWidgetService::onGetViewFactory")
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MarkdownRemoteViewsFactory(applicationContext, appWidgetId)
        } else {
            MarkdownRemoteViewsFactory(applicationContext, appWidgetId)
        }
    }

    /**
     * Remote Views Factory, used by the Service.
     */
    internal inner class MarkdownRemoteViewsFactory(private val context: Context, private val appWidgetId: Int) :
        MarkdownRemoteViewsFactoryBase(context, appWidgetId) {

        /**
         * No idea if the Annotation is sufficient to warrant compatibility with older versions.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        override fun getLoadingView(): RemoteViews {
            return RemoteViews(context.packageName, R.layout.markdown_file_widget, R.id.emptyView)
        }

        /**
         * TODO: What should the Item Id represent?
         */
        @RequiresApi(Build.VERSION_CODES.S)
        override fun getItemId(position: Int): Long {
            Log.v(TAG, "MarkdownRemoteViewsFactory::getItemId")
            return id * 10 + position // (getViewAt(position).viewId).toLong()
        }
    }

    /**
     * Abstract Base class containing a large part of the functionality.
     */
    internal abstract inner class MarkdownRemoteViewsFactoryBase(private val context: Context, private val appWidgetId: Int
    ) : RemoteViewsFactory {
        private val cachedMarkdown: SparseArray<MarkdownRenderer> = MarkdownFileWidget.cachedMarkdown
        private var width: Int = 0
        private var height: Int = 0
        private var bitmap: Bitmap? = null
        protected var id: Long = 0

        /**
         * Invoked on creation.
         */
        override fun onCreate() {
            Log.i(TAG, "MarkdownRemoteViewsFactory::onCreate")
            updateContent()
        }

        /**
         * Invoked if data set has changed. IF THIS IS TO BE USED: does not run in main thread.
         */
        override fun onDataSetChanged() {
            Log.i(TAG, "MarkdownRemoteViewsFactory::onDataSetChanged")
        }

        /**
         * Invoked on destruction.
         */
        override fun onDestroy() {
            Log.i(TAG, "MarkdownRemoteViewsFactory::onDestroy")
            // bitmaps.clear()
        }

        /**
         * Returns number of items in the bitmaps list of bitmaps.
         *
         * @return number of items
         */
        override fun getCount(): Int {

            val factory = this
            var count = 0
            bitmap?.apply {
                val lastPixel = this[width - 1, height - 1]
                val (x, y) = indexOf { pixel -> pixel != lastPixel }
                val targetHeight = maxOf(y + WIDGET_PADDING, factory.height)
                count = (targetHeight + STRIPE_HEIGHT - 1) / STRIPE_HEIGHT
            }

            Log.i(TAG, "MarkdownRemoteViewsFactory::getCount => ${count}")
            return count
        }

        /**
         * Returns a single row for a given position.
         *
         * @return A single view with a bitmap stripe
         */
        override fun getViewAt(position: Int): RemoteViews {
            Log.v(TAG, "MarkdownRemoteViewsFactory::getViewAt[$position]")
            val remoteView = RemoteViews(context.packageName, R.layout.widget_item)
            this.bitmap?.apply {
                val bitmap = extractBitmap(0, position * STRIPE_HEIGHT, width, STRIPE_HEIGHT)
                remoteView.setImageViewBitmap(R.id.imageViewItem, bitmap)
            }
            remoteView.setOnClickFillInIntent(R.id.imageViewItem, prefs.getEditIntent(context, appWidgetId))

            // Removed code that sets the other fields as I tried it with and without and it didn't matter. So removed for brevity
            return remoteView
        }

        /**
         * We support only a single type - the bitmap representing a stripe of a bigger whole.
         */
        override fun getViewTypeCount(): Int {
            return 1
        }

        /**
         * TODO: What does it mean and how is it bo be used?
         */
        override fun hasStableIds(): Boolean {
            return false
        }

        /**
         * Using a reference to the Cached Markdown, extracts the last Bitmap, immediately calculated
         * before this invocation.
         */
        private fun updateContent() {
            id++
            val size = WidgetSizeProvider(context)
            val (width, height) = size.getWidgetsSize(appWidgetId)
            this.width = width
            this.height = height
            val md = cachedMarkdown[appWidgetId]
            bitmap = md?.getBitmap(width, height)
        }

        /**
         * Divides a larger Bitmap into a set of stripes, each 100 pixels high.
         */
        private fun getBitmaps() : MutableList<Bitmap> {
            bitmap?.apply {
                val rowHeight = STRIPE_HEIGHT
                val rows = (height + rowHeight - 1) / rowHeight
                val bitmaps: MutableList<Bitmap> = listOf<Bitmap>().toMutableList()
                for (row in 1..rows) {
                    val y = (row - 1) * rowHeight
                    val availableHeight =
                        if (rowHeight > height - y) height - y else rowHeight
                    bitmaps.add(row - 1, extractBitmap(0, y, width, availableHeight))
                }
                return bitmaps
            }
            return listOf<Bitmap>().toMutableList()
        }
    }
}

/**
 * Returns the Intent to be used to request the invocation of the Markdown editor.
 * Depends on the configured method during Widget creation.
 * TODO: obsidian not reacting.
 *
 * @param appWidgetId the id of the App Widget initiating this request
 * @return pending intent
 */
private fun Preferences.getEditPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {

    val pendingFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    val intent = getEditIntent(context, appWidgetId)
    return PendingIntent.getActivity(context, 0, intent, pendingFlags)
}

/**
 * The Intent to invoke the Markdown editor.
 */
private fun Preferences.getEditIntent(context: Context, appWidgetId: Int): Intent? {

    val uri = markdownUriOf(appWidgetId)
    val tapBehavior = this[appWidgetId, PREF_BEHAVIOUR, TAP_BEHAVIOUR_DEFAULT_APP]

    val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    val intent = Intent(Intent.ACTION_EDIT).apply {
        this.flags = flags
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        when (tapBehavior) {
            TAP_BEHAVIOUR_DEFAULT_APP -> setDataAndType(uri.normalizeScheme(), "text/plain")
            TAP_BEHAVIOUR_OBSIDIAN -> data = uri.toObsidian(context)
            TAP_BEHAVIOUR_ITSELF -> {
                setComponent(ComponentName(context.packageName, MainActivity::class.qualifiedName!!))
                setDataAndType(uri.normalizeScheme(), "text/plain")
                // data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            else -> { return null }
        }
    }

    Log.d(TAG, "Intent assembled: ${intent.resolveActivity(context.packageManager)}, data : ${intent.data}, extras: ${intent.extras}, tap: $tapBehavior")
    return intent
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

/* TEST CODE WITH LIST VIEW

views.apply {
    val rowHeight = 100
    val builder = RemoteViews.RemoteCollectionItems.Builder()
    val rows = (bitmap.height + rowHeight - 1) / rowHeight
    for (row in 1..rows) {
        val y = (row - 1) * rowHeight
        val availableHeight = if (rowHeight > bitmap.height - y) bitmap.height - y else rowHeight
        val singleRow = RemoteViews(context.packageName, R.layout.widget_item)
        singleRow.setImageViewBitmap(R.id.imageViewItem, bitmap.extractBitmap(0, y, width, availableHeight))
        builder.addItem(row.toLong(), singleRow)

        singleRow.setOnClickPendingIntent(
            R.id.imageViewItem,
            getIntent(context, appWidgetId)
        )
        appWidgetManager.updateAppWidget(appWidgetId, singleRow)
    }
    builder
        .setViewTypeCount(1)
        //.setHasStableIds(true)
    val itemCollection = builder.build()
    setRemoteAdapter(R.id.renderImg, itemCollection)
}

appWidgetManager.updateAppWidget(appWidgetId, views)

 TEST END
 */
