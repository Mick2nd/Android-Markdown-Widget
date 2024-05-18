package ch.tiim.markdown_widget.di

import android.content.Context
import android.os.Environment
import ch.tiim.markdown_widget.ExternalStoragePathHandlerAlt
import ch.tiim.markdown_widget.FileChecker
import dagger.BindsInstance
import dagger.Component
import dagger.Provides
import javax.inject.Singleton

/**
 * Serves as the CONTAINER for the DI
 */
@Component(modules = [AppModule::class, ContextModule::class, StringModule::class])
@Singleton
interface AppComponent {
    companion object {
        lateinit var instance: AppComponent
        var isCreated = false

        /**
         * Supports one time creation of the singleton instance
         * Succeeding requests must be done per instance
         * @param context the app context
         * @param type the folder type to be used as initial folder for permission request
         * @return the created (singleton) instance
         */
        fun create(context: Context, type: String) : AppComponent {
            if (!isCreated) {
                instance = DaggerAppComponent
                    .builder()
                    .contextModule(ContextModule(context))
                    .stringModule(StringModule(type))
                    .build()

                with(instance) {
                    inject(externalStoragePathHandler())
                    externalStoragePathHandler().restoreState()
                }
                isCreated = true
            }
            return instance
        }
    }

    /* COULD NOT WORK WITH THESE APPROACHES
     *
    @Component.Builder
    interface Builder {
        // @BindsInstance
        fun contextModule(context: Context) : Builder

        // @BindsInstance
        fun stringModule(type: String) : Builder

        fun build() : AppComponent
    }

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context, @BindsInstance type: String) : AppComponent
    }
     */

    @Singleton
    fun externalStoragePathHandler() : ExternalStoragePathHandlerAlt

    fun inject(handler: ExternalStoragePathHandlerAlt)
}
