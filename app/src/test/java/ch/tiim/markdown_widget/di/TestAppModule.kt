package ch.tiim.markdown_widget.di

import android.content.Context
import android.net.Uri
import ch.tiim.markdown_widget.ChangeObserver
import ch.tiim.markdown_widget.Preferences
import ch.tiim.markdown_widget.fakes.FakePreferences
import ch.tiim.markdown_widget.fakes.SpyPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.migration.DisableInstallInCheck
import javax.inject.Named
import javax.inject.Singleton

/**
 * Currently not active.
 */
@Module(includes = [])
@DisableInstallInCheck
// @TestInstallIn(
//    components = [SingletonComponent::class],
//    replaces = [AppModule::class]
// )
open class TestAppModule {
    @Provides
    @Singleton
    fun provideFileChecker(@ApplicationContext context: Context, @Named("GLOBAL") uri: Uri) : ChangeObserver {
        return ChangeObserver(context, uri)
    }

    @Provides
    @Singleton
    @Named("MOCK")
    fun provideMockPreferences(@ApplicationContext context: Context) : FakePreferences {
        return FakePreferences(context)
    }

    @Provides
    @Singleton
    // @Named("SPY")
    fun provideSpyPreferences(@ApplicationContext context: Context) : Preferences {
        return SpyPreferences(context)
    }
}
