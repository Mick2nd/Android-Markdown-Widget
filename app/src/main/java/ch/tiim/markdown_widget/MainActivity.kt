package ch.tiim.markdown_widget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import ch.tiim.markdown_widget.di.AppComponent

private const val DEBUG = true
private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var appComponent: AppComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.coffee).setOnClickListener(View.OnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/Tiim"))
            startActivity(browserIntent)
        })
    }

    override fun onStart() {
        super.onStart()
        // THIS PIECE OF CODE WORKS, INCLUDING THE INVOCATION OF THE CALLBACK
        with(AppComponent.instance) {
            storagePermissionChecker().requestAccess(this@MainActivity) {
                displayOnDebug()
            }
        }
    }

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
            val debugLayout = findViewById<LinearLayout>(R.id.debugLayout)
            debugLayout.addView(
                MarkdownRenderer(
                    applicationContext,
                    debugLayout.measuredWidth,
                    debugLayout.measuredHeight,
                    testTxt
                ).webView
            )
        }
    }
}
