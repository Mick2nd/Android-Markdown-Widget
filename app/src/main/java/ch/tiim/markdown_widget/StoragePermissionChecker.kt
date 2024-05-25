package ch.tiim.markdown_widget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "StoragePermissionChecker"

@Singleton
class StoragePermissionCheckerImpl @Inject constructor(
    val context: Context,
    val type: String,
    private val prefs: Preferences) : StoragePermissionChecker {

    /**
     * THIS SECTION IS A TRIAL TO GAIN ACCESS TO A USER SELECTED FOLDER
     * PREFERRED the Public Documents Folder
     * On completion an internal callback is invoked updating the state of this instance
     * @param activity sometimes it is necessary to have a reference to the client activity
     * @param onReady displays the Debug content. optional.
     * @return returns the Uri of the selected folder or null, not used at the moment
     */
    override fun requestAccess(activity: AppCompatActivity, onReady: (folderUri: Uri) -> Unit) {
        prefs.userFolderUri?.let {
            onReady(it)
            return
        }
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
                                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    prefs.userFolderUri = it
                                    resumed = true
                                    onReady(it)

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
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addCategory(Intent.CATEGORY_DEFAULT)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
                    }
                }
                resultLauncher.launch(intent)
            }
        }
    }
}

/**
 * This interface is used by DI framework Dagger
 */
interface StoragePermissionChecker {

    fun requestAccess(activity: AppCompatActivity, onReady: (folderUri: Uri) -> Unit = { })

}
