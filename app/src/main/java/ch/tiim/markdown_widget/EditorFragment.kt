package ch.tiim.markdown_widget

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import ch.tiim.markdown_widget.databinding.FragmentEditorBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Timer
import javax.inject.Inject

private const val TAG = "EditorFragment"

/**
 * A simple [Fragment] subclass.
 * Use the [EditorFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class EditorFragment : Fragment() {

    @Inject lateinit var contentCache: ContentCache
    @Inject lateinit var prefs: Preferences
    private lateinit var binding: FragmentEditorBinding

    private var uri: Uri? = null
    private var appWidgetId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            uri = it.getString(ARG_URI)?.let { s -> Uri.parse(s) }
            appWidgetId = it.getInt(ARG_APP_WIDGET_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?) : View {

        // Inflate the layout for this fragment
        binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uri?.let {
            binding.markdownEditText.setText(it.load(requireContext()))
            binding.floatingActionButton.isEnabled = it.scheme == "content"
            adaptLineNumbers()
        }
        binding.floatingActionButton.setOnClickListener {
            uri?.let {
                try {
                    contentCache.store(it,binding.markdownEditText.text.toString())                 // to cache, disk
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
            adaptSelection()
        }
        binding.markdownEditText.setOnScrollChangeListener {
            _, _, scrollY, _, _ ->
            adaptSelection()
            adaptVisibleArea()
        }
        binding.markdownEditText.setOnKeyListener { _, _, _ -> false }
    }

    private fun adaptLineNumbers() {
        val text = binding.markdownEditText.text.toString()
        val lineCount = binding.markdownEditText.getLineNumbers()
        val lineCountOld = binding.lineNumbersEditText.getLineNumbers()
        if (lineCount == lineCountOld) {
            return
        }
        val range = 1 .. lineCount
        val lineNumbers = range.map { i -> i.toString().padStart(3, ' ') }.reduce() {
                n1, n2 -> "$n1\n$n2"
        }
        binding.lineNumbersEditText.setText(lineNumbers)
    }

    private fun adaptVisibleArea() {
        binding.lineNumbersEditText.verticalScrollbarPosition = binding.markdownEditText.verticalScrollbarPosition
    }

    private fun adaptSelection() {
        val pos = binding.markdownEditText.selectionStart
        val text = binding.markdownEditText.text.toString().substring(0, pos)
        val line = text.count { c -> c == '\n' }
        binding.lineNumbersEditText.setSelection(line * 4)
    }

    private fun EditText.getLineNumbers() : Int {
        val text = text.toString()
        val count = text.count { c -> c == '\n' } + 1
        return count
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
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