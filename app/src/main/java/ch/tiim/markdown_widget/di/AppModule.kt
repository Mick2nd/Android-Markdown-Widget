package ch.tiim.markdown_widget.di

import android.content.Context
import android.net.Uri
import androidx.webkit.WebViewAssetLoader
import ch.tiim.markdown_widget.ContentCache
import ch.tiim.markdown_widget.ContentCacheImpl
import ch.tiim.markdown_widget.ExternalStoragePathHandler
import ch.tiim.markdown_widget.ExternalStoragePathHandlerAlt
import ch.tiim.markdown_widget.FileServices
import ch.tiim.markdown_widget.Preferences
import ch.tiim.markdown_widget.StoragePermissionChecker
import ch.tiim.markdown_widget.StoragePermissionCheckerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.support.AndroidSupportInjectionModule
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "DAGGER_LOG"

/**
 * Provides all the Singleton instances for the MAIN COMPONENT
 */
@Module(includes = [
    AppModule.Bindings::class,
    AndroidSupportInjectionModule::class
])
@InstallIn(SingletonComponent::class)
open class AppModule {

    @Provides
    @Singleton
    fun provideFileChecker(@ApplicationContext context: Context, @Named("GLOBAL") uri: Uri) : FileServices {
        return FileServices(context, uri)
    }

    @Provides
    @Singleton
    open fun providePreferences(@ApplicationContext context: Context) : Preferences {
        return Preferences(context)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    interface Bindings {

        @Binds
        @Singleton
        fun provideStoragePermissionChecker(impl: StoragePermissionCheckerImpl) : StoragePermissionChecker

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
        fun provideContentCache(impl: ContentCacheImpl) :ContentCache
    }
 }
