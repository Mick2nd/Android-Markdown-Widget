package ch.tiim.markdown_widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.Log.i
import androidx.core.graphics.get
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

/**
 * Escapes the Newlines in a string.
 */
fun String.escapeNl() : String {
    return replace("\n", "\\n")
}

/**
 * An alternate toString method for a Bitmap. Returns the class name and dimensions.
 */
fun Bitmap.toStringAlt() : String {
    val cls = toString().substringAfterLast('.')
    return "$cls : $width x $height"
}

/**
 * A reverse iterator through the pixels of a Bitmap.
 */
operator fun Bitmap.iterator() : Iterable<Int> {
    val bm = this
    return Iterable {
        iterator {
            for (y in height - 1 downTo  0) {
                for (x in width - 1 downTo 0) {
                    yield(bm[x, y])
                }
            }
        }
    }
}

/**
 * Returns the complete information of a Bitmap as Bundle.
 */
fun Bitmap.toBundle() : Bundle {
    val bundle = Bundle()
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    bundle.putInt("width", width)
    bundle.putInt("height", height)
    bundle.putIntArray("pixels", pixels)

    return bundle
}

/**
 * Returns meta data of a Bitmap as Bundle. This can be used to identify the Bitmap for rendering
 * purposes.
 */
fun Bitmap.toBundleMeta() : Bundle {
    val bundle = Bundle()
    bundle.putInt("width", width)
    bundle.putInt("height", height)
    bundle.putString("pixels", "$this")

    return bundle
}

/**
 * Restores a Bitmap from the information previously created as Bundle.
 */
fun Bundle.fromBundle() : Bitmap {
    val width = getInt("width")
    val height = getInt("height")
    val pixels = getIntArray("pixels")
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels!!, 0, width, 0, 0, width, height)

    return bitmap
}

/**
 * Searches the pixels of a Bitmap from the end until a condition is met. Returns the found pixel
 * coordinates. Use case: we search for the last pixel containing information.
 *
 * @param predicate predicate to apply to the pixels
 * @return pair of coordinates x, y
 */
fun Bitmap.indexOf(predicate: (Int) -> Boolean) : Pair<Int, Int> {
    val heightOffset = 0 // height / 2                                                              // used for optimization
    val noPixels = width * (height - heightOffset)
    val pixels = IntArray(noPixels)
    getPixels(pixels,0, width,0, heightOffset, width, height - heightOffset)
    val lastPixel = pixels[noPixels - 1]
    val pos = pixels.indexOfLast { px -> px != lastPixel }
    if (pos >= 0) {
        val y = pos / width + heightOffset
        val x = pos % width
        return x to y
    }

    throw IllegalStateException("Bitmap empty")
}

/**
 * Extracts a part of a greater Bitmap.
 * Usage Recipe:
 * - build a greater Bitmap, a part of which is to be extracted.
 * - define location and part
 */
fun Bitmap.extractBitmap(x: Int, y: Int, width: Int, height: Int): Bitmap {
    val wl = width.coerceIn(0, this.width)
    val hl = height.coerceIn(0, this.height)
    val xl = x.coerceIn(0, this.width - wl)
    val yl = y.coerceIn(0, this.height - hl)

    val newBitmap = Bitmap.createBitmap(wl, hl, Bitmap.Config.ARGB_8888)        // Create a same size Bitmap
    val pixels = IntArray(wl * hl)
    getPixels(pixels, 0, wl, xl, yl, wl, hl)
    newBitmap.setPixels(pixels, 0, wl, 0, 0, wl, hl)
    return newBitmap
}

/**
 * Logs Thread and Stack information on demand.
 */
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
