package ch.tiim.markdown_widget.di

import android.content.Context
import android.net.Uri
import androidx.webkit.WebViewAssetLoader
import ch.tiim.markdown_widget.ExternalStoragePathHandler
import ch.tiim.markdown_widget.ExternalStoragePathHandlerAlt
import ch.tiim.markdown_widget.FileServices
import ch.tiim.markdown_widget.Preferences
import ch.tiim.markdown_widget.StoragePermissionChecker
import ch.tiim.markdown_widget.StoragePermissionCheckerImpl
import ch.tiim.markdown_widget.FileContentObserver
import ch.tiim.markdown_widget.FileContentObserverImpl
import ch.tiim.markdown_widget.createStylesObserver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "DAGGER_LOG"

/**
 * Provides all the Singleton instances for the MAIN COMPONENT
 */
@Module(includes = [AppModule.Bindings::class])
@DisableInstallInCheck
open class AppModule {

    @Provides
    @Singleton
    fun provideFileChecker(context: Context, @Named("GLOBAL") uri: Uri) : FileServices {
        return FileServices(context, uri)
    }

    @Provides
    @Singleton
    open fun providePreferences(context: Context) : Preferences {
        return Preferences(context)
    }

    @Provides
    @Singleton
    @Named("GLOBAL-2")
    fun provideFileContentObserver2(context: Context, @Named("GLOBAL") uri: Uri) = createStylesObserver(context, uri)

    @Module
    @DisableInstallInCheck
    interface Bindings {

        @Binds
        @Singleton
        @Named("EXTERNAL")
        fun provideExternalStoragePathHandler(impl: ExternalStoragePathHandler) : WebViewAssetLoader.PathHandler

        @Binds
        @Singleton
        @Named("GLOBAL")
        fun provideExternalStoragePathHandlerAlt(impl: ExternalStoragePathHandlerAlt) : WebViewAssetLoader.PathHandler

        @Binds
        @Singleton
        fun provideStoragePermissionChecker(impl: StoragePermissionCheckerImpl) : StoragePermissionChecker

        @Binds
        @Singleton
        @Named("GLOBAL-1")
        fun provideFileContentObserver1(impl: FileContentObserverImpl) : FileContentObserver
    }
 }
