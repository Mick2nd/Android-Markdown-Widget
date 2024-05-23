package ch.tiim.markdown_widget.di

import android.content.Context
import ch.tiim.markdown_widget.ExternalStoragePathHandlerAlt
import ch.tiim.markdown_widget.ExternalStoragePathHandlerAltImpl
import ch.tiim.markdown_widget.FileChecker
import ch.tiim.markdown_widget.Preferences
import ch.tiim.markdown_widget.StoragePermissionChecker
import ch.tiim.markdown_widget.StoragePermissionCheckerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

private const val TAG = "DAGGER_LOG"

/**
 * Provides all the Singleton instances for the MAIN COMPONENT
 */
@Module(includes = [AppModule.Bindings::class])
class AppModule {

    @Provides
    @Singleton
    fun provideFileChecker(context: Context, prefs: Preferences) : FileChecker {
        return FileChecker(context, prefs)
    }

    @Provides
    @Singleton
    fun providePreferences(context: Context) : Preferences {
        return Preferences(context)
    }

    @Module
    interface Bindings {
        @Binds
        @Singleton
        fun provideExternalStoragePathHandler(impl: ExternalStoragePathHandlerAltImpl) : ExternalStoragePathHandlerAlt

        @Binds
        @Singleton
        fun provideStoragePermissionChecker(impl: StoragePermissionCheckerImpl) : StoragePermissionChecker
    }
 }
