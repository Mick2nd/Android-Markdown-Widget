package ch.tiim.markdown_widget

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ch.tiim.markdown_widget.databinding.FragmentPreviewBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "PreviewFragment"

/**
 * A [Fragment] serving as Preview window for the rendered markdown.
 * Use the [PreviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class PreviewFragment : Fragment(), ChangeSignal {

    @Inject lateinit var contentCache: ContentCache
    @Inject lateinit var prefs: Preferences
    private lateinit var binding: FragmentPreviewBinding

    private var uri: Uri? = null
    private var appWidgetId: Int = 0

    /**
     * Init block.
     */
    init {
        Log.i(TAG, "Init block")
    }

    /**
     * [onCreate] override. Used to read the arguments of this fragment.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
        arguments?.let {
            uri = it.getString(ARG_URI)?.let { s -> Uri.parse(s) }
            appWidgetId = it.getInt(ARG_APP_WIDGET_ID)
        }
    }

    /**
     * [onCreateView] override. Used to bind the embedded views.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) : View {
        Log.i(TAG, "onCreateView")
        binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * [onViewCreated] override. Performs initial display.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated")
        show()
    }

    /**
     * [ChangeSignal] interface method. Used to update the Preview with new content.
     */
    override fun signal() {
        if (uri == null) {
            return
        }
        binding.previewLayout.removeAllViews()
        show()
    }

    /**
     * Shows the markdown content given by an Uri.
     */
    private fun show() {
        val markdownFromWidget = uri?.let {
            contentCache[it]
        } ?: ""
        binding.previewLayout.addView(
            MarkdownRenderer(
                requireActivity().applicationContext,
                markdownFromWidget
            ).webView
        )
    }

    /**
     * Can be used to support arguments for this Preview [Fragment].
     */
    companion object {
        /**
         * Use this factory method to create a new instance of this fragment using the provided
         * parameters.
         *
         * @return A new instance of fragment PreviewFragment.
         */
        @JvmStatic
        fun newInstance(uri: Uri?, appWidgetId: Int) =
            PreviewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URI, uri?.toString())
                    putInt(ARG_APP_WIDGET_ID, appWidgetId)
                }
            }
    }
}