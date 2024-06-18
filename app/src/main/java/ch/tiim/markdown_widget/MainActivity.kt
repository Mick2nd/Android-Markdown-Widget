package ch.tiim.markdown_widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import ch.tiim.markdown_widget.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// the fragment initialization parameters
const val ARG_URI = "arg_uri"
const val ARG_APP_WIDGET_ID = "arg_app_widget_id"

private const val TAG = "MainActivity"

/**
 * The main activity invoked when app is invoked. It can also be invoked by clicking on a widget.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ChangeSignal {

    @Inject lateinit var permissionChecker: StoragePermissionChecker
    private lateinit var fragments: List<Fragment>
    private lateinit var binding: ActivityMainBinding

    init {
        Log.i(TAG, "Init block")
    }

    /**
     * [onCreate] Override.
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // I check the Intent: if it was initiated by the Markdown Widget
        // If so, start with the Edit fragment
        var uri: Uri? = null
        var appWidgetId = 0
        var start = 0
        intent.action?.let {
            if (it == Intent.ACTION_EDIT) {
                appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)
                uri = intent.data
                start = 2
            }
        }

        fragments = listOf(                                                                         // initialize the fragments
            MainFragment(),
            ConfigureFragment(),
            EditorFragment.newInstance(uri, appWidgetId),
            PreviewFragment.newInstance(uri, appWidgetId)
        )
        val tabLayout = binding.tabLayout
        val viewPager = binding.viewPager
        val adapter = AdapterTabPager(fragments, activity = this)

        viewPager.isUserInputEnabled = false
        viewPager.adapter = adapter
        viewPager.setCurrentItem(start, true)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = adapter.getTabTitle(position)
        }.attach()

        permissionChecker.requestAccess(this)                                                // request the user for access to folder with userstyle.css
    }

    /**
     * Implements the ChangeSignal interface. The signal signals changes in the Editor fragment.
     * This is used to propagate changes to the Preview fragment.
     */
    override fun signal() {
        val idx = fragments.indexOfFirst { fragment -> fragment is ChangeSignal }
        binding.viewPager.setCurrentItem(idx, true)
        (fragments[idx] as ChangeSignal).signal()
    }

    /**
     * This adapter manages the association between [TabLayout] tabs and [Fragment]s to be displayed.
     */
    class AdapterTabPager(
        private val fragments: List<Fragment>,
        activity: FragmentActivity?) : FragmentStateAdapter(activity!!
        ) {
        fun getTabTitle(position : Int): String {
            return when (position) {
                0 -> "Main"
                1 -> "Configure"
                2 -> "Edit"
                else -> "Preview"
            }
        }

        override fun getItemCount(): Int {
            return fragments.count()
        }

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }
    }
}

/**
 * Interface to support change signals.
 */
interface ChangeSignal {
    fun signal()
}
