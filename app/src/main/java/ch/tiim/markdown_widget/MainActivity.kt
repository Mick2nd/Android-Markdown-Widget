package ch.tiim.markdown_widget

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


private const val TAG = "MainActivity"

/**
 * The main activity invoked when app is invoked.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var permissionChecker: StoragePermissionChecker

    init {
        Log.i(TAG, "Init block")
    }

    /**
     * [onCreate] Override.
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val adapter = AdapterTabPager(activity = this)

        viewPager.adapter = adapter
        viewPager.currentItem = 0
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = adapter.getTabTitle(position)
        }.attach()

        permissionChecker.requestAccess(this)
    }

    class AdapterTabPager(activity: FragmentActivity?) : FragmentStateAdapter(activity!!) {

        private val fragments = listOf(MainFragment(), ConfigureFragment())

        public fun getTabTitle(position : Int): String {
            return when (position) {
                0 -> "Main"
                else -> "Configure"
            }
        }

        override fun getItemCount(): Int {
            return 2
        }

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }
    }
}
