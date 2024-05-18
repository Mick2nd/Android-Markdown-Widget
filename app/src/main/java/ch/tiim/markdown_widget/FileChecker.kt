package ch.tiim.markdown_widget

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject

private const val TAG = "FileChecker"

/**
 * This class is meant as checker for the *userstyle.css* file changes
 * The check has the same relevance as the check of Markdown changes
 * Actually the location of this file is in the INTERNAL storage of this app
 * @param uri the complete Uri of the file to be checked
 */
class FileChecker @Inject constructor (val context: Context, val uri: Uri) {

    var err: Exception? = null
    private var state: String = ""

    init {
        updateState()
    }

    fun updateState() {
        state = loadFile()
    }

    fun stateChanged(): Boolean {
        return loadFile() != state
    }

    private fun loadFile(): String {
        try {
            val ins: InputStream = context.contentResolver.openInputStream(uri)!!
            val reader = BufferedReader(InputStreamReader(ins, "utf-8"))
            val data = reader.lines().reduce { s, t -> s + "\n" + t }
            reader.close()
            ins.close()
            err = null
            return data.get()
        } catch (err: FileNotFoundException) {
            this.err = err
            Log.w(TAG, err.toString())
            return ""
        } catch (err: Exception) {
            this.err = err
            Log.w(TAG, err.toString())
            return ""
        } finally {
        }
    }
}
