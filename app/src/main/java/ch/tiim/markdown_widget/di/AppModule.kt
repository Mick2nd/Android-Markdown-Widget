package ch.tiim.markdown_widget.di

import ch.tiim.markdown_widget.ExternalStoragePathHandlerAlt
import ch.tiim.markdown_widget.ExternalStoragePathHandlerAltImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

private const val TAG = "DAGGER_LOG"

@Module(includes = [AppModule.Bindings::class])
class AppModule {
    // THIS SELF-IMPLEMENTATION OF SINGLETON IS DONE BY DAGGER WITH BINDS, SEE BELOW
    // private val instance: ExternalStoragePathHandlerAlt by lazy { ExternalStoragePathHandlerAlt() }

    // @Provides
    // @Singleton
    // fun provideExternalStoragePathHandler(): ExternalStoragePathHandlerAlt = instance

    @Module
    interface Bindings {
        @Binds
        @Singleton
        fun provideExternalStoragePathHandler(impl: ExternalStoragePathHandlerAltImpl): ExternalStoragePathHandlerAlt
    }
 }
