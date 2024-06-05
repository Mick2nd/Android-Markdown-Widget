package ch.tiim.markdown_widget

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val DEBUG = true
private const val TAG = "MainActivity"

/**
 * The main activity invoked when app is invoked.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var permissionChecker: StoragePermissionChecker
    @Inject lateinit var prefs: Preferences

    /**
     * [onCreate] Override.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.coffee).setOnClickListener(View.OnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/Tiim"))
            startActivity(browserIntent)
        })

        findViewById<Button>(R.id.revoke).setOnClickListener(View.OnClickListener {
            prefs.revokeUserFolderPermission()
        })

        val (width, height) = getScreenSize()                                                       // this "settings" are used globally for DEBUG view
        prefs[SCREEN_WIDTH] = width.toString()                                                      // and all widgets
        prefs[SCREEN_HEIGHT] = height.toString()

        permissionChecker.requestAccess(this) {
            displayOnDebug()
        }
    }

    /**
     * [onStart] Override. Used to request access to an user selected global folder.
     */
    override fun onStart() {
        super.onStart()
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

            Log.d("Test:", testTxt)
            val debugLayout = findViewById<ConstraintLayout>(R.id.debugLayout)
            debugLayout.addView(
                MarkdownRenderer(
                    applicationContext,
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
            val windowMetrics = windowManager.currentWindowMetrics
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
}
