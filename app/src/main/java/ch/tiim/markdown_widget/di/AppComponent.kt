package ch.tiim.markdown_widget.di

import android.content.Context
import ch.tiim.markdown_widget.ExternalStoragePathHandlerAlt
import ch.tiim.markdown_widget.FileChecker
import ch.tiim.markdown_widget.Preferences
import ch.tiim.markdown_widget.StoragePermissionChecker
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

/**
 * Serves as the MAIN CONTAINER for the DI
 */
@Component(modules = [AppModule::class, UriModule::class])
@Singleton
interface AppComponent {
    companion object {
        lateinit var instance: AppComponent

        /**
         * Supports one time creation of the singleton instance
         * Succeeding requests must be done per instance
         * @param context the app context
         * @param type the folder type to be used as initial folder for permission request
         * @return the created (singleton) instance
         */
        fun create(context: Context, type: String) : AppComponent {
            instance = DaggerAppComponent.factory().create(context, type)
            return instance
        }
    }

    @Singleton
    fun externalStoragePathHandler() : ExternalStoragePathHandlerAlt

    @Singleton
    fun storagePermissionChecker() : StoragePermissionChecker

    @Singleton
    fun fileChecker() : FileChecker

    @Singleton
    fun preferences() : Preferences

    fun inject(handler: ExternalStoragePathHandlerAlt)

    fun inject(checker: StoragePermissionChecker)

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context, @BindsInstance type: String) : AppComponent
    }
}
