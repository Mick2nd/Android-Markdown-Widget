package ch.tiim.markdown_widget.di

import android.net.Uri
import ch.tiim.markdown_widget.MainActivity
import ch.tiim.markdown_widget.MarkdownFileWidgetConfigureActivity
import dagger.BindsInstance
import dagger.Component

/**
 * Component with [ActivityScope].
 * Intent is to provide dependencies for Activities per injection
 */
@Component(
    modules = [ActivityModule::class],
    dependencies = [AppComponent::class]
)
@ActivityScope
interface ActivityComponent {

    fun uri() : Uri

    fun inject(activity: MarkdownFileWidgetConfigureActivity)

    fun inject(activity: MainActivity)

    @Component.Factory
    interface Factory {
        /**
         * Factory of the component with [ActivityScope]
         */
        @ActivityScope
        fun create(
            @BindsInstance path: String,
            applicationComponent: AppComponent
        ) : ActivityComponent
    }
}
