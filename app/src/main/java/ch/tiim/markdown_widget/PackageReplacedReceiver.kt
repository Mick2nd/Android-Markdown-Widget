package ch.tiim.markdown_widget

import android.app.AlertDialog
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MY_PACKAGE_REPLACED
import android.content.Intent.ACTION_PACKAGE_REPLACED
import android.os.Build
import android.os.Debug
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi

private const val TAG = "PackageReplacedReceiver"
class PackageReplacedReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context?, intent: Intent?) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        // TODO("PackageReplacedReceiver.onReceive() is not implemented")
        Toast.makeText(context, "Hello", Toast.LENGTH_LONG).show()
        if (intent!!.action == ACTION_MY_PACKAGE_REPLACED) {
            Log.i("UpdateService", "Package Replaced Receiver onReceive")
            val updateViewModel = UpdateViewModel(context as Application)
            updateViewModel.startService()
        } else {
            Log.w("UpdateService", "Package Replaced Receiver onReceive unexpected broadcast")
            // throw Exception("Invalid Broadcast intent")
        }
    }
}
