package ch.tiim.markdown_widget

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MY_PACKAGE_REPLACED
import android.util.Log
import android.widget.Toast

private const val TAG = "PackageReplacedReceiver"

/**
 * Handles the following broadcasts:
 * - [ACTION_MY_PACKAGE_REPLACED] (new installation of the app)
 * - BOOT_COMPLETED (reboot of the system)
 *
 * Intention was to restart the update service because that's not done automatically.
 */
class PackageReplacedReceiver : BroadcastReceiver() {

    /**
     * onReceive override. Handles incoming broadcasts.
     *
     * @param context the context
     * @param intent the intent of the broadcast
     */
    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent!!.action == ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "Package Replaced Receiver onReceive")
            Toast.makeText(context, "Restarted the update service", Toast.LENGTH_LONG).show()
            val updateViewModel = UpdateViewModel(context as Application)
            updateViewModel.startService()
        } else {
            Log.w(TAG, "Package Replaced Receiver onReceive unexpected broadcast")
        }
    }
}
