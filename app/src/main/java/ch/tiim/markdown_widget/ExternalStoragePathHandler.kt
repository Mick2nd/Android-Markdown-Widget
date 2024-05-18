package ch.tiim.markdown_widget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.WebResourceResponse
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.provider.DocumentsContractCompat
import androidx.documentfile.provider.DocumentFile
import androidx.webkit.WebViewAssetLoader
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "ExternalStoragePathHandler"
private const val ENCODED_FOLDER_URI = "encodedFolderUri"
private const val PREFS_NAME = "ch.tiim.markdown_widget.MarkdownFileWidget"

/**
 * TRIAL to use external (shared) storage for the userstyle.css file
 * SUCCEEDED
 * @param context the context
 * @param subFolder here the sub folder of the files folder
 */
class ExternalStoragePathHandler(
    val context: Context,
    val subFolder: String
    ) : WebViewAssetLoader.PathHandler {

    /**
     * Implements the PathHandler interface
     * @param path file name of the intercepted file
     * @return a web resource response
     */
    override fun handle(path: String): WebResourceResponse {
        try {
            val dir = context.getExternalFilesDir(subFolder)
            dir?.mkdirs()
            val file = File(dir, path)
            val uri = Uri.fromFile(file)
            val stream = context.contentResolver.openInputStream(uri)
            return WebResourceResponse("text/css", "utf-8", stream)
        } catch (err: Exception) {
            throw err
        }
    }
}

/**
 * Goal of this class is to support access of WebView to files in SHARED EXTERNAL STORAGE
 * Implemented as SINGLETON
 */
@Singleton
class ExternalStoragePathHandlerAltImpl @Inject constructor() : WebViewAssetLoader.PathHandler, ExternalStoragePathHandlerAlt {

    @Inject lateinit var context: Context
    @Inject lateinit var type: String
    private var folderUri: Uri? = null
    private var isReady: Boolean = false
    private var isInjected = true

    /**
     * Inject dependencies per method injection
     * THIS METHOD *inject* IS NO LONGER RESPONSIBLE FOR INJECTION -> DAGGER
     * @param context the context
     * @param type the type of folder we want access to
     */

    /**
     * THIS SECTION IS A TRIAL TO GAIN ACCESS TO A USER SELECTED FOLDER
     * PREFERRED the Public Documents Folder
     * On completion an internal callback is invoked updating the state of this instance
     * @param activity sometimes it is necessary to have a reference to the client activity
     * @param display displays the Debug content. optional.
     * @return returns the Uri of the selected folder or null, not used at the moment
     */
    override fun requestAccess(activity: AppCompatActivity, display: () -> Unit) : Unit {
        assert(isInjected)
        if (restoreState()) {
            display()
            return
        }
        isReady = false
        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            suspendCoroutine<Boolean> { continuation ->
                val resultLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    result ->
                    var resumed = false
                    try {
                        if (result.resultCode == Activity.RESULT_OK) {
                            val data: Intent? = result.data
                            data?.data?.also {
                                try {
                                    Log.i(TAG, "Request of OPEN_DOCUMENT_TREE succeeded: $it")
                                    folderUri = it
                                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    saveState()
                                    isReady = true
                                    resumed = true
                                    display()

                                    // TEST
                                    // THIS invocation permanently returns null
                                    try {
                                        getInputStream("userstyle.css")
                                    }
                                    catch(err: Throwable) {
                                        Log.w(TAG, "$err")
                                    }
                                    // TEST END

                                    continuation.resume(true)
                                } catch (err: Exception) {
                                    Log.e(TAG, "$err")
                                    resumed = true
                                    continuation.resumeWithException(err)
                                }
                            }
                        } else {
                            Toast.makeText(context.applicationContext, "User Canceled Selection", Toast.LENGTH_LONG).show()
                            Log.w(TAG, "Request of OPEN_DOCUMENT_TREE did not succeed: ${result.resultCode}")
                        }
                    }
                    finally {
                        if (!resumed) {
                            continuation.resume(false)
                        }
                    }
                }

                val file = Environment.getExternalStoragePublicDirectory(type)
                val uri = Uri.fromFile(file)
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    addCategory(Intent.CATEGORY_DEFAULT);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
                    }
                }
                resultLauncher.launch(intent)
            }
        }
    }

    override fun restoreState() : Boolean {
        with (context.getSharedPreferences(PREFS_NAME, 0)) {
            val encodedUri = getString(ENCODED_FOLDER_URI, null)
            encodedUri?.let {
                folderUri = Uri.parse(Uri.decode(encodedUri))
                Log.i(TAG, "Decoded Folder Uri from app state")
                return true
            }
        }
        return false
    }

    private fun saveState() {
        with (context.getSharedPreferences(PREFS_NAME, 0)) {
            edit()
                .putString(ENCODED_FOLDER_URI, Uri.encode(folderUri.toString()))
                .apply()
            Log.i(TAG, "Stored encoded Folder Uri from app state")
        }
    }

    /**
     * Implements the PathHandler interface
     * @param path file name of the intercepted file
     * @return a web resource response
     */
    override fun handle(path: String): WebResourceResponse {
        try {
            assert(isInjected)
            val stream = getInputStream(path)
            return WebResourceResponse("text/css", "utf-8", stream)
        } catch (err: Exception) {
            throw err
        }

        // return WebResourceResponse(null, null, null)
    }

    /**
     * Intentionally opens and returns an Input Stream
     * THIS FUN DID NOT WORK IN ANY CASE
     * @param path the filename of the document to open
     * @return the opened Input Stream
     */
    private fun getInputStream(path: String) : InputStream {
        var stream: InputStream? = null
        val uri = DocumentsContract.buildDocumentUriUsingTree(folderUri, query(path))
        stream = (context.contentResolver).openInputStream(uri)
        Log.i(TAG, "Have a document file at $path: ${uri != null}")
        Log.i(TAG, "Opened Input Stream at $uri: ${stream != null}")
        return stream!!
    }

    /**
     * This query has the task of providing a document id for a given display name
     * The file in question must exist in the folder with uri folderUri
     * @param path the display name of the file or file name
     * @return the document id
     * @throws FileNotFoundException
     */
    private fun query(path: String) : String {
        val treeUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, DocumentsContract.getTreeDocumentId(folderUri))
        val cursor = context.contentResolver.query(
            treeUri,
            arrayOf(OpenableColumns.DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            "${OpenableColumns.DISPLAY_NAME} = ?",
            arrayOf(path),
            null)
        if (cursor != null && cursor.moveToNext()) {
            val documentId = cursor.getString(1)
            cursor.close()
            return documentId
        }

        cursor?.close()
        throw FileNotFoundException("Document $path not found")
    }
}

/**
 * This interface is used by DI framework Dagger
 */
interface ExternalStoragePathHandlerAlt {
    fun requestAccess(activity: AppCompatActivity, display: () -> Unit = { }) : Unit
    fun restoreState() : Boolean
}
