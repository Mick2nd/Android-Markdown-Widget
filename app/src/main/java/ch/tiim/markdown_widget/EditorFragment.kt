package ch.tiim.markdown_widget

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import ch.tiim.markdown_widget.databinding.FragmentEditorBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "EditorFragment"
private const val CHARACTERS_PER_LINE = 4

/**
 * A [Fragment] serving as simple Editor for Markdown content. One of the pages on the Main Activity.
 * Use the [EditorFragment.newInstance] factory method to create an instance of this fragment.
 */
@AndroidEntryPoint
class EditorFragment : Fragment() {

    @Inject lateinit var contentCache: ContentCache
    @Inject lateinit var prefs: Preferences
    private lateinit var binding: FragmentEditorBinding

    private var uri: Uri? = null
    private var appWidgetId: Int = 0

    /**
     * The [onCreate] override, reads the arguments.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            uri = it.getString(ARG_URI)?.let { s -> Uri.parse(s) }
            appWidgetId = it.getInt(ARG_APP_WIDGET_ID)
        }
    }

    /**
     * The [onCreateView] override, establishes the binding for the views.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?) : View {

        // Inflate the layout for this fragment
        binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * The [onViewCreated] override, performs final initialization: initial text content, button
     * handlers, adaptation(synchronization) of the line numbers Edit Text.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uri?.let {
            binding.markdownEditText.setText(contentCache[it])
            binding.floatingActionButton.isEnabled = it.scheme == "content"
            adaptLineNumbers()
            adaptSelection()
        }
        binding.floatingActionButton.setOnClickListener {
            uri?.let {
                try {
                    contentCache.store(it, binding.markdownEditText.text.toString())                // to cache, disk
                    getUpdatePendingIntent(view.context, appWidgetId).send()                        // to widget
                    (activity as ChangeSignal).signal()                                             // to preview
                    Toast.makeText(
                        view.context,
                        "Markdown content written back to disk",
                        Toast.LENGTH_SHORT).show()
                } catch(err: Throwable) {
                    Toast.makeText(
                        view.context,
                        "$err",
                        Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Exception during save: $err")
                }
            }
        }
        binding.markdownEditText.addTextChangedListener {
            _ ->
            adaptLineNumbers()
        }
        binding.markdownEditText.setOnScrollChangeListener {
            _, _, scrollY, _, _ ->
            binding.lineNumbersEditText.scrollY = scrollY
        }
        binding.markdownEditText.accessibilityDelegate = object: View.AccessibilityDelegate() {
            override fun sendAccessibilityEvent(host: View, eventType: Int) {
                super.sendAccessibilityEvent(host, eventType)
                if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                    adaptSelection()
                }
            }
        }
    }

    /**
     * Adapts the displayed line numbers to the number of lines in text area.
     */
    private fun adaptLineNumbers() {
        val lineCount = binding.markdownEditText.getLineNumbers()
        val lineCountOld = binding.lineNumbersEditText.getLineNumbers()
        if (lineCount == lineCountOld) {
            return
        }
        val range = 1 .. lineCount
        val lineNumbers = range.map { i -> i.toString().padStart(CHARACTERS_PER_LINE - 1, ' ') }.reduce() {
                n1, n2 -> "$n1\n$n2"
        }
        binding.lineNumbersEditText.setText(lineNumbers)
    }

    /**
     * Adapts the position of cursor in the line numbers view to that of the text area (e.g. the
     * line of the cursor)
     */
    private fun adaptSelection(line: Int = 0) {
        val pos = binding.markdownEditText.selectionStart
        val text = binding.markdownEditText.text.toString().substring(0, pos)
        val target = if (line > 0) line else text.count { c -> c == '\n' }
        binding.lineNumbersEditText.setSelection(target * CHARACTERS_PER_LINE)
    }

    /**
     * Determines and returns the number of lines in an [EditText] view.
     */
    private fun EditText.getLineNumbers() : Int {
        val text = text.toString()
        val count = text.count { c -> c == '\n' } + 1
        return count
    }

    companion object {
        /**
         * Use this factory method to create a new instance of this fragment using the provided
         * parameters.
         *
         * @return A new instance of fragment EditorFragment.
         */
        @JvmStatic
        fun newInstance(uri: Uri?, appWidgetId: Int) =
            EditorFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URI, uri?.toString())
                    putInt(ARG_APP_WIDGET_ID, appWidgetId)
                }
            }
    }
}