package ch.tiim.markdown_widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import ch.tiim.markdown_widget.databinding.MarkdownFileWidgetConfigureBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

internal const val TAP_BEHAVIOUR_NONE = "none"
internal const val TAP_BEHAVIOUR_DEFAULT_APP = "default_app"
internal const val TAP_BEHAVIOUR_OBSIDIAN = "obsidian"

private const val ACTIVITY_RESULT_BROWSE = 1

/**
 * The configuration screen for the [MarkdownFileWidget] AppWidget.
 */
@AndroidEntryPoint
class MarkdownFileWidgetConfigureActivity @Inject constructor() : AppCompatActivity() {

    @Inject lateinit var prefs: Preferences

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var inputFilePath: EditText
    private lateinit var radioGroup: RadioGroup
    private lateinit var binding: MarkdownFileWidgetConfigureBinding

    /**
     * OnClickListener handler for browse button.
     */
    private val onBrowse = View.OnClickListener {
        // Workaround for https://github.com/Tiim/Android-Markdown-Widget/issues/14:
        // Check if MIME-Type "text/markdown" is known. Otherwise fall back to
        // generic type to still allow file selection.
        val mimetype = if (MimeTypeMap.getSingleton().hasMimeType("text/markdown")) {
            "text/markdown"
        } else {
            "*/*"
        }
        // https://developer.android.com/reference/android/content/Intent#ACTION_OPEN_DOCUMENT
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimetype
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION.or( Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(Intent.createChooser(intent, "Select a markdown file"), ACTIVITY_RESULT_BROWSE)
    }

    /**
     * [onActivityResult] override.
     */
    @Suppress
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if( requestCode == ACTIVITY_RESULT_BROWSE && resultCode == RESULT_OK && data?.data != null) {
            val uri: Uri = data.data!!;

            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val context = this@MarkdownFileWidgetConfigureActivity
            val text = uri.toString()
            inputFilePath.setText(text.toCharArray(), 0, text.length)

            prefs[appWidgetId, PREF_FILE] = text
        }
    }

    /**
     * OnClickListener handler for Add Widget button.
     */
    private val onAddWidget = View.OnClickListener {
        val context = this@MarkdownFileWidgetConfigureActivity

        // When the button is clicked, store the string locally
        val widgetText = inputFilePath.text.toString()
        prefs[appWidgetId, PREF_FILE] = widgetText

        val rID = radioGroup.checkedRadioButtonId
        val tapBehaviour = when (rID) {
            R.id.radio_noop -> {
                TAP_BEHAVIOUR_NONE
            }
            R.id.radio_obsidian -> {
                TAP_BEHAVIOUR_OBSIDIAN
            }
            else -> {
                TAP_BEHAVIOUR_DEFAULT_APP
            }
        }
        prefs[appWidgetId, PREF_BEHAVIOUR] = tapBehaviour


        // It is the responsibility of the configuration activity to update the app widget
        val appWidgetManager = AppWidgetManager.getInstance(context)

        //appWidgetManager.updateAppWidget(appWidgetId, RemoteViews(context.packageName, R.layout.markdown_file_widget ))
        getUpdatePendingIntent(context, appWidgetId).send()

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)

        val updateViewModel = UpdateViewModel(application)
        updateViewModel.startService()

        finish()
    }

    /**
     * [onCreate] override. Handles the creation of this activity.
     */
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        binding = MarkdownFileWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inputFilePath = binding.inputFile
        radioGroup = binding.radiogroup
        binding.addButton.setOnClickListener(onAddWidget)
        binding.btnBrowse.setOnClickListener(onBrowse)
        binding.radioDefaultApp.isSelected = true

        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
    }
}
