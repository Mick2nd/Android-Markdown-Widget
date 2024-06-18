package ch.tiim.markdown_widget

import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import ch.tiim.markdown_widget.databinding.FragmentConfigureBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val DEBUG = true
private const val TAG = "ConfigureFragment"

/**
 * This fragment supports application wide settings and services. In Debug mode it enables the
 * display of a markdown sample.
 * Use the [ConfigureFragment.newInstance] factory method to create an instance of this fragment.
 */
@AndroidEntryPoint
class ConfigureFragment : Fragment() {

    @Inject lateinit var permissionChecker: StoragePermissionChecker
    @Inject lateinit var contentCache: ContentCache
    @Inject lateinit var prefs: Preferences
    private lateinit var binding: FragmentConfigureBinding

    /**
     * [onCreate] override. Nothing special.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    /**
     * [onCreateView] override. Used here to create the view and establish the binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) : View {
        binding = FragmentConfigureBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * [onViewCreated] override. Contains the main functionality. Button click handler and parameter
     * reading / writing.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.revoke.setOnClickListener {
            prefs.revokeUserFolderPermission()
            Toast.makeText(view.context, "Permission for user folder revoked", Toast.LENGTH_SHORT)
                .show()
        }

        binding.refresh.setOnClickListener {
            try {
                contentCache.refresh()
                Log.d(TAG, "Widgets to be refreshed: ${prefs.widgetIds()}")
                for (appWidgetId in prefs.widgetIds()) {
                    getUpdatePendingIntent(view.context, appWidgetId).send()
                }
                Toast.makeText(view.context, "All widgets refreshed", Toast.LENGTH_SHORT).show()
            } catch (err: Throwable) {
                contentCache.clean()
                Toast.makeText(view.context, "$err", Toast.LENGTH_LONG).show()
            }
        }

        binding.useUserStyle.let {                                                       // TODO: propagate changes to Preview and Wídgets
            it.isChecked = prefs.useUserStyle
            it.setOnClickListener { _ ->
                prefs.useUserStyle = it.isChecked
                displayOnDebug()
            }
        }

        val spinner = binding.zoom
        val adapter = ArrayAdapter.createFromResource(
            view.context,
            R.array.zoom_factors,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)          // Specify the layout to use when the list of choices appears.
            spinner.adapter = adapter                                                               // Apply the adapter to the spinner.
        }
        spinner.also {
            val zoom = (prefs.zoom * 100f).toInt().toString()
            val pos = adapter.getPosition(zoom)
            it.setSelection(pos)
            it.onItemSelectedListener = listener
        }

        val (width, height) = getScreenSize()                                                       // these "settings" are used globally for DEBUG view
        prefs[SCREEN_WIDTH] = width.toString()                                                      // and all widgets
        prefs[SCREEN_HEIGHT] = height.toString()

        displayOnDebug()
    }

    /**
     * [onDestroy] override.
     */
    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * Displays a sample md page in Debug mode.
     */
    private fun displayOnDebug() {
        if (DEBUG) {
            val testTxt = """
                [TOC]
                # Test
                
                ## Table
                        
                |Column 1|Column 2|
                |-|-|
                |Item 1.1|Item 1.2|
                [Sample]
                
                ## Code Block...
                
                > ### This Quote may be a longer paragraph
                > How can this be styled? We use a css style here
                > See the *default.css*
                > > #### A nested Quote
                > > Working?
                
                ```xml
                    <root>
                        <item name="Jürgen">Sample Text</item>
                        <item name="Regina">Sample Text</item>
                    </root>
                ```

                ```mermaid
                sequenceDiagram
                participant John
                participant Alice
                Alice->>John: Hello John, how are you?
                John-->>Alice: Great
                ```
                
                The symbol $\mathbb N$ is the symbol for natural numbers.
                
                ```math
                \Gamma(n) = (n-1)!\quad\forall n\in\mathbb N
                ```
                
                ## Admonition
                
                !!! abstract
                    ### Here a Title
                    
                    Hello this is the abstract text.
                                    
                ## List
                
                1. this is a list
                1. list entry 2
                
                - [x] task 1 
                - [ ] task 2
                
                ## Subscript, Superscript
                
                A~1~, B^2^
                
            """.trimIndent()
            val debugLayout = binding.debugLayout
            debugLayout.addView(
                MarkdownRenderer(
                    requireActivity().applicationContext,
                    testTxt
                ).webView
            )
        }
    }

    /**
     * Returns the Dimensions of the Screen. This is required for proper scaling of the WebView
     * contents.
     *
     * @return Dimensions in Pixels
     */
    private fun getScreenSize() : Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = requireActivity().windowManager.currentWindowMetrics
            val width = windowMetrics.bounds.width()
            val height = windowMetrics.bounds.height()

            width to height
        } else {
            val displayMetrics = Resources.getSystem().displayMetrics
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels

            width to height
        }
    }

    /**
     * Zoom Spinner listener.
     */
    private val listener = object : AdapterView.OnItemSelectedListener {
        /**
         *
         * Callback method to be invoked when an item in this view has been selected. This callback
         * is invoked only when the newly selected position is different from the previously selected
         * position or if there was no selected item.
         *
         * Implementers can call getItemAtPosition(position) if they need to access the data
         * associated with the selected item.
         *
         * @param parent The AdapterView where the selection happened
         * @param view The view within the AdapterView that was clicked
         * @param position The position of the view in the adapter
         * @param id The row id of the item that is selected
         */
        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            prefs.zoom = ( parent?.selectedItem ?: "70" ).toString().toFloat() / 100f               // TODO: propagate ?
            displayOnDebug()
        }

        /**
         * Callback method to be invoked when the selection disappears from this view. The selection
         * can disappear for instance when touch is activated or when the adapter becomes empty.
         *
         * @param parent The AdapterView that now contains no selected item.
         */
        override fun onNothingSelected(parent: AdapterView<*>?) {
            // TODO("Not yet implemented")
        }
    }

    /**
     * Can be used to support parameters.
     */
    companion object {
        /**
         * Use this factory method to create a new instance of this fragment using the provided
         * parameters.
         *
         * @return A new instance of fragment ConfigureFragment.
         */
        @JvmStatic
        fun newInstance() =
            ConfigureFragment().apply {
            }
    }
}