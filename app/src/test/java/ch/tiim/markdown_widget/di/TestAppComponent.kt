package ch.tiim.markdown_widget.di

import android.app.Application
import android.content.Context
import ch.tiim.markdown_widget.Preferences
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(modules = [ TestAppModule::class, TestStringModule::class, TestUriModule::class ])
interface TestAppComponent : AppComponent
{
    @Singleton
    @Named("MOCK")
    override fun preferences() : Preferences

    @Component.Factory
    @Singleton
    interface Factory {
        @Singleton
        fun create(@BindsInstance app: Application, @BindsInstance context: Context, @BindsInstance type: String) : TestAppComponent
    }
}
