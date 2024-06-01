package ch.tiim.markdown_widget.di

import android.net.Uri
import ch.tiim.markdown_widget.MainActivity
import ch.tiim.markdown_widget.MarkdownFileWidgetConfigureActivity
import dagger.Subcomponent

/**
 * Component with [ActivityScope].
 * Intent is to provide dependencies for Activities per injection
 */
@Subcomponent(
    modules = [ActivityModule::class]
)
@ActivityScope
interface ActivityComponent {

    fun inject(activity: MarkdownFileWidgetConfigureActivity)

    fun inject(activity: MainActivity)

    @Subcomponent.Factory
    interface Factory {
        /**
         * Factory of the component with [ActivityScope]
         */
        @ActivityScope
        fun create(
        ) : ActivityComponent
    }
}
