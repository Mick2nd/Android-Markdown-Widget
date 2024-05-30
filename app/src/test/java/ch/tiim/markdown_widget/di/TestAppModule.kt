package ch.tiim.markdown_widget.di

import android.content.Context
import ch.tiim.markdown_widget.MockPreferences
import ch.tiim.markdown_widget.Preferences
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
open class TestAppModule : AppModule() {

    @Provides
    @Singleton
    @Named("MOCK")
    fun provideMockPreferences(context: Context) : Preferences {
        return MockPreferences(context)
    }
}
