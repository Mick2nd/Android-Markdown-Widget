package ch.tiim.markdown_widget

import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doOnTextChanged
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

private const val DEBUG = true

/**
 * A simple [Fragment] subclass.
 * Use the [ConfigureFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class ConfigureFragment : Fragment() {

    @Inject lateinit var permissionChecker: StoragePermissionChecker
    @Inject lateinit var contentCache: ContentCache
    @Inject lateinit var prefs: Preferences

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_configure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.revoke)?.setOnClickListener(View.OnClickListener {
            prefs.revokeUserFolderPermission()
            Toast.makeText(view.context, "Permission for user folder revoked", Toast.LENGTH_SHORT).show()
        })

        view.findViewById<Button>(R.id.refresh)?.setOnClickListener(View.OnClickListener {
            contentCache.refresh()
            for (appWidgetId in prefs.widgetIds()) {
                getUpdatePendingIntent(view.context, appWidgetId).send()
            }
            Toast.makeText(view.context, "All widgets refreshed", Toast.LENGTH_SHORT).show()
        })

        view.findViewById<CheckBox>(R.id.useUserStyle)?.let {
            it.isChecked = prefs.useUserStyle
            it.setOnClickListener { _ ->
                prefs.useUserStyle = it.isChecked
                displayOnDebug()
            }
        }

        view.findViewById<EditText>(R.id.zoom).let {
            it.text = Editable.Factory.getInstance().newEditable((prefs.zoom * 100f).toInt().toString())
            it.doOnTextChanged { text, _, _, _ ->
                prefs.zoom = (text ?: "70").toString().toFloat() / 100f
                displayOnDebug()
            }
        }

        val (width, height) = getScreenSize()                                                       // this "settings" are used globally for DEBUG view
        prefs[SCREEN_WIDTH] = width.toString()                                                      // and all widgets
        prefs[SCREEN_HEIGHT] = height.toString()

        displayOnDebug()
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
                        
                |1|2|
                |-|-|
                |1|2|
                [Sample]
                
                ## Code Block...
                
                > ### This Quote may be a longer paragraph
                > How can this be styled? We use a css style here
                > See the *default.css*
                > > #### A nested Quote
                > > Working?
                
                ```xml
                    <root>
                        <item name="Jürgen">Sample Text</item
                        <item name="Regina">Sample Text</item
                    </root>
                ```

                ```mermaid
                sequenceDiagram
                participant John
                participant Alice
                Alice->>John: Hello John, how are you?
                John-->>Alice: Great
                ```
                
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

            val debugLayout = view?.findViewById<ConstraintLayout>(R.id.debugLayout)
            Log.d("Test:", testTxt)
            Log.d("Test", "debugLayout is $debugLayout")
            debugLayout?.addView(
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = requireActivity().windowManager.currentWindowMetrics
            val width = windowMetrics.bounds.width()
            val height = windowMetrics.bounds.height()

            return width to height
        } else {
            val displayMetrics = Resources.getSystem().displayMetrics
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels

            return width to height
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ConfigureFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ConfigureFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}