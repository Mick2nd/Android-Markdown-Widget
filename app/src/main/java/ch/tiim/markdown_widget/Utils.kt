package ch.tiim.markdown_widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Log
import android.util.Log.i
import androidx.core.text.htmlEncode
import kotlin.reflect.KClass

private const val TAG = "Utils"

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
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val (width, height) = listOf(
            if (isPortrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
            else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
            if (isPortrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
            else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
        ).map { manager.getAppWidgetOptions(widgetId).getInt(it, 100).px.toInt() }

        Log.d(TAG, "Device size: $width $height")
        return width to height
    }

    /**
     * This method calculates the ratio between widget width and screen width. This is used to
     * perform widget updates in the index.html's javascript.
     */
    fun getWidgetWidthRatio(widgetId: Int, prefs: Preferences) : Float {
        val manager = AppWidgetManager.getInstance(context)
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val widthSetting =
            if (isPortrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
            else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH

        val width = manager.getAppWidgetOptions(widgetId).getInt(widthSetting, -1).toFloat()
        val screenWidth = prefs[SCREEN_WIDTH, "1000"].toFloat()
        val result = if (width > 0) width.px / screenWidth  else 0.9f
        Log.d(TAG, "Calculated width ratio: $result, Screen width: $screenWidth")
        return result
    }

    private val Number.px: Float get() = this.toFloat() * Resources.getSystem().displayMetrics.density
    private val Number.dp: Float get() = this.toFloat() / Resources.getSystem().displayMetrics.density
}

/**
 * Executes the given [block] and returns elapsed time in milliseconds.
 */
fun measureTimeMillis(block: () -> Unit): Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}

/**
 * Can be used to define application wide "begin" of a string.
 */
fun String.begin()  : String {
    return substring(0, 80)
}

fun String.escapeNl() : String {
    return replace("\n", "\\n")
}

fun Bitmap.toStringAlt() : String {
    val cls = toString().substringAfterLast('.')
    return "$cls : $width x $height"
}

fun KClass<Log>.thread(point: Int, additionalInfo: String = "") {
    val noEntries = 4
    val msg = Thread.currentThread().let {
        val stackTrace =
            it.stackTrace
            .slice(3 .. noEntries + 2)
            .joinToString("\n") { elem -> "$elem" }
        "$point: ${it.name} : $additionalInfo =>\n$stackTrace"
    }
    i("THREADING", msg)
}
