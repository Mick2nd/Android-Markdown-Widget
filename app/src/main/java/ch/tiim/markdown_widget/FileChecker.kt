package ch.tiim.markdown_widget

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader

private const val TAG = "FileChecker"
class FileChecker (val context: Context, val uri: Uri) {

    private var state: String = ""
    private var err: Exception? = null

    init {
        updateState()
    }

    fun updateState() {
        state = loadFile()
    }

    fun stateChanged(): Boolean {
        return loadFile() != state
    }

    fun getException(): Exception? {
        return err
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
            Log.w(TAG, err.toString())
            return ""
        } catch (err: Exception) {
            Log.w(TAG, err.toString())
            return ""
        } finally {
        }
    }
}
