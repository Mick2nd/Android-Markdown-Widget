package ch.tiim.markdown_widget

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.util.Log
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "FileContentObserver"

/**
 * GLOBAL-1 variant of the FileContentObserver is using [ContentObserver]. It watches the file
 * *userstyle.css* in GLOBAL external storage.
 * STATUS: NOT WORKING, events do not arrive.
 *
 * @param context the context
 * @param uri the Uri of the file as injected by dagger
 */
class FileContentObserverImpl @Inject constructor(val context: Context, @Named("GLOBAL") val uri: Uri) :
    ContentObserver(Handler(Looper.getMainLooper())), FileContentObserver {

    private var handler: ((Context) -> Unit)? = null

    /**
     * Init block.
     */
    init {
        observe()
    }

    /**
     * Permits injection of a handler to be executed on file change.
     */
    override fun injectHandler(handler: (Context) -> Unit) {
        this.handler = handler
    }

    override fun onChange(selfChange: Boolean) {
        onChange(selfChange, null, 0)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        onChange(selfChange, null, 0)
    }

    /**
     * onChange event.
     */
    override fun onChange(selfChange: Boolean, uri: Uri?, flags: Int) {
        // super.onChange(selfChange, uri, flags)
        updateSettings()
    }

    /**
     * Registers this Observer.
     */
    private fun observe() {
        val resolver = context.contentResolver
        resolver.registerContentObserver(uri, false, this)
    }

    /**
     * Executes the change handler (if configured)
     */
    private fun updateSettings() {
        Log.d(TAG, "File Changed")
        handler?.let { it(context) }
    }
}

/**
 * Does the real work: watching for file changes of the userstyle.css file and performing widget updates.
 * STATUS: NOT WORKING WITH FILES with content scheme, e.g. *userstyle.css* in Documents folder.
 */
fun createStylesObserver(context: Context, uri: Uri) : FileContentObserver {

    val o =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            object : FileObserver(uri.toFile2(), MODIFY or CREATE), FileContentObserver {
                private lateinit var handler: (Context) -> Unit

                override fun onEvent(event: Int, path: String?) {
                    Log.i(TAG, "UserStyle.css updated")
                    handler(context)
                }

                override fun injectHandler(handler: (Context) -> Unit) {
                    this.handler = handler
                }
            } else object : FileObserver(uri.toString(), MODIFY or CREATE), FileContentObserver {
                private lateinit var handler: (Context) -> Unit

                override fun onEvent(event: Int, path: String?) {
                    Log.i(TAG, "UserStyle.css updated")
                    handler(context)
                }

                override fun injectHandler(handler: (Context) -> Unit) {
                    this.handler = handler
            }
        }
    return o
}

interface FileContentObserver {
    fun injectHandler(handler: (Context) -> Unit)
}
