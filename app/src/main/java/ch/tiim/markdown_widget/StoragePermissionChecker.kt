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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "StoragePermissionChecker"

/**
 * Checks for and acquires user permission to an user provided folder. This folder is meant as the
 * location where the *userstyle.css* file resides.
 */
@Singleton
class StoragePermissionCheckerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("SUBFOLDER") private val type: String,
    private val prefs: Preferences) : StoragePermissionChecker {

    /**
     * THIS SECTION IS A TRIAL TO GAIN ACCESS TO A USER SELECTED FOLDER.
     * PREFERRED the Public Documents Folder.
     * On completion an internal callback is invoked updating the state of this instance.
     *
     * @param activity sometimes it is necessary to have a reference to the client activity
     * @param onReady displays the Debug content. optional.
     */
    override fun requestAccess(activity: AppCompatActivity, onReady: () -> Unit) {
        if (!prefs.useUserStyle) {
            onReady()
            return
        }
        prefs.userFolderUri?.let {
            onReady()
            return
        }
        val resultLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    data?.data?.also {
                        try {
                            Log.i(TAG, "Request of OPEN_DOCUMENT_TREE succeeded: $it")
                            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            prefs.userFolderUri = it
                            onReady()
                        } catch (err: Exception) {
                            Log.e(TAG, "$err")
                        }
                    }
                } else {
                    Toast.makeText(context.applicationContext, "User Canceled Selection", Toast.LENGTH_LONG).show()
                    Log.w(TAG, "Request of OPEN_DOCUMENT_TREE did not succeed: ${result.resultCode}")
                    onReady()
                }
        }

        resultLauncher.launch(openDocumentTreeIntent)
    }

    /**
     * Returns the Intent for opening the document tree activity.
     */
    private val openDocumentTreeIntent: Intent
        get() {
            val file = Environment.getExternalStoragePublicDirectory(type)
            val uri = Uri.fromFile(file)
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addCategory(Intent.CATEGORY_DEFAULT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
                }
            }
            return intent
        }
}

/**
 * This interface is used by DI framework Dagger
 */
interface StoragePermissionChecker {

    fun requestAccess(activity: AppCompatActivity, onReady: () -> Unit = { })

}
