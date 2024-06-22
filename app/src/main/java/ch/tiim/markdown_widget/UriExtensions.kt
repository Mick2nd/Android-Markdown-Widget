package ch.tiim.markdown_widget

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "Uri Extensions"


/**
 * Extension of [Uri]. Extracts the display name.
 *
 * @receiver Uri
 * @param context the [Context]. Provides the [ContentResolver].
 * @return the display name
 */
fun Uri.displayName(context: Context) : String? {
    var result: String? = null
    if (scheme == "content") {
        val cursor: Cursor? = context.contentResolver.query(
            this,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null, null, null)
        cursor.use { c ->
            if (c != null && c.moveToFirst()) {
                result = c.getString(0)
            }
        }
    }
    return result
}

/**
 * Extension of [Uri]. Extracts the file name (e.g. last section of path).
 *
 * @receiver Uri
 * @return the file name
 */
fun Uri.fileName() : String? {
    val result = path
    result?.apply {
        val cut = lastIndexOf('/')
        if (cut != -1) {
            return substring(cut + 1)
        }
    }
    return result
}

/**
 * Extension of [Uri]. Transforms the Uri to obsidian scheme.
 *
 * @receiver Uri
 * @param context the [Context]. Provides the [ContentResolver].
 * @return the obsidian Uri
 */
fun Uri.toObsidian(context: Context) : Uri {

    var result = displayName(context)
    if (result == null) {
        result = fileName()
    }
    val obsidianUri = "obsidian://open?file=${Uri.encode(result)}"
    Log.d(TAG, "Obsidian invocation with: $obsidianUri")
    return Uri.parse(obsidianUri)
}

/**
 * An improvement of the Kotlin version. That version cannot convert Uris with 'content' scheme.
 * This is just a trial.
 *
 * @receiver Uri with 'content' or 'file' scheme
 * @return File instance
 */
fun Uri.toFile2() : File {
    path?.let {
        val idx = it.lastIndexOf("primary:")
        if (idx >= 0) {
            val documentPath = it.substring(idx).replace("primary:", "///")
            val fileSystemPath = it.substring(idx).replace("primary:", "///mnt/sdcard/")
            val uri = Uri.Builder().scheme("file").path(fileSystemPath).build()
            Log.d(TAG, "${uri}, ${uri.toFile()}")
            return uri.toFile()
        }
    }
    throw FileNotFoundException("Uri not supported: $this")
}

/**
 * Loads a text file given the [Uri]. The Uri must have one of the schemes *content*, *http*, *https*.
 *
 * @receiver Uri the uri
 * @return the content to be returned
 * @throws FileNotFoundException
 * @throws IllegalArgumentException
 */
fun Uri.load(context: Context) : String {
    if (scheme in arrayOf("http", "https")) {
        return loadUrl()
    }
    return loadContent(context)
}

/**
 * Loads a text file from the Internet using the http or https protocols.
 *
 * @receiver Uri the uri
 * @return the content to be returned
 * @throws FileNotFoundException
 * @throws IllegalArgumentException
 */
fun Uri.loadUrl() : String {
    if (scheme in arrayOf("http", "https")) {
        val url = URL(toString())
        return url.loadUrl()
    }
    throw IllegalArgumentException("Illegal scheme $scheme for loadUrl")
}

/**
 * Loads a text file from the Internet using the http or https protocols.
 *
 * @receiver URL the url
 * @return the content to be returned
 * @throws FileNotFoundException
 */
fun URL.loadUrl() : String {
    Log.d(TAG, "About to read from Url")
    val url = this
    var result = ""
    return runBlocking {
        withContext(Dispatchers.IO) {
            val parent = coroutineContext[Job]
            val deferred = async {
                val text = url.readSync()
                text
            }
            val job = launch {
                delay(5000)
                Log.w(TAG, "About to cancel")
                parent?.cancel("Timeout")                                                   // TODO: does not work as expected, can hang in await
            }
            result = deferred.await()
            job.cancel("Data ready")
        }
        Log.d(TAG, "Result from Web: ${result.begin()}")
        result
    }
}

/**
 * Synchronous read from http server.
 */
private fun URL.readSync() : String {
    return openConnection().run {
        this as HttpURLConnection
        val text: String
        try {
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Invalid response from server: $responseMessage")
            }
            text = inputStream.bufferedReader().readText()
        } finally {
            inputStream.bufferedReader().close()
            inputStream.close()
            disconnect()
        }
        text
    }
}

/**
 * Loads a text file from local file storage using the content scheme.
 *
 * @receiver Uri the uri
 * @return the content to be returned
 * @throws FileNotFoundException
 * @throws IllegalArgumentException
 */
fun Uri.loadContent(context: Context) : String {
    if (scheme in arrayOf("content")) {
        val receiver = this
        return runBlocking {
            async(Dispatchers.IO) {
                val ins: InputStream = context.contentResolver.openInputStream(receiver)!!
                val reader = BufferedReader(InputStreamReader(ins, "utf-8"))
                val text = reader.lines().reduce { s, t -> s + "\n" + t }
                reader.close()
                ins.close()
                text.get()
            } .await()
        }
    }
    if (scheme == null) {
        return ""
    }
    throw IllegalArgumentException("Illegal scheme $scheme for loadContent")
}

fun Uri.store(context: Context, text: String) {
    storeContent(context, text)
}

fun Uri.storeContent(context: Context, text: String) {
    if (scheme in arrayOf("content")) {
        val receiver = this
        return runBlocking {
            launch(Dispatchers.IO) {
                val outs: OutputStream = context.contentResolver.openOutputStream(receiver)!!
                val writer = BufferedWriter(OutputStreamWriter(outs, "utf-8"))
                writer.write(text)
                writer.close()
                outs.close()
            }
        }
    }
    if (scheme == null) {
        return
    }
    throw IllegalArgumentException("Illegal scheme $scheme for storeContent")
}
