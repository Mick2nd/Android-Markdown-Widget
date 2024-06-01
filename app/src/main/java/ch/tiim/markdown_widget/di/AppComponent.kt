package ch.tiim.markdown_widget.di

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.webkit.WebViewAssetLoader
import ch.tiim.markdown_widget.FileServices
import ch.tiim.markdown_widget.Main
import ch.tiim.markdown_widget.MarkdownFileWidget
import ch.tiim.markdown_widget.MarkdownRenderer
import ch.tiim.markdown_widget.Preferences
import ch.tiim.markdown_widget.StoragePermissionChecker
import ch.tiim.markdown_widget.UpdateService
import ch.tiim.markdown_widget.FileContentObserver
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

/**
 * Serves as the MAIN CONTAINER for the DI
 */
@Component(modules = [AppModule::class, UriModule::class, StringModule::class])
@Singleton
interface AppComponent {
    companion object {
        lateinit var instance: AppComponent

        /**
         * Supports one time creation of the singleton instance
         * Succeeding requests must be done per instance
         *
         * @param context the app context
         * @param type the folder type to be used as initial folder for permission request
         * @return the created (singleton) instance
         */
        fun create(app: Application, context: Context) : AppComponent {
            instance = DaggerAppComponent.factory().create(app, context)
            return instance
        }
    }

    @Singleton
    @Named("EXTERNAL")
    fun externalStoragePathHandler() : WebViewAssetLoader.PathHandler

    @Singleton
    @Named("GLOBAL")
    fun externalStoragePathHandlerAlt() : WebViewAssetLoader.PathHandler

    @Singleton
    fun storagePermissionChecker() : StoragePermissionChecker

    @Singleton
    fun fileChecker() : FileServices

    @Singleton
    fun preferences() : Preferences

    @Singleton
    @Named("GLOBAL")
    fun uri() : Uri

    @Singleton
    @Named("GLOBAL-1")
    fun fileContentObserver() : FileContentObserver

    fun activityComponentFactory() : ActivityComponent.Factory

    /**
     * Invocation in app widget did not work.
     * Reason: proper type must be provided for injection to work.
     */
    fun inject(widget: MarkdownFileWidget)

    fun inject(renderer: MarkdownRenderer)

    fun inject(service: UpdateService)

    fun inject(main: Main)

    @Component.Factory
    @Singleton
    interface Factory {
        @Singleton
        fun create(@BindsInstance app: Application, @BindsInstance context: Context) : AppComponent
    }
}
