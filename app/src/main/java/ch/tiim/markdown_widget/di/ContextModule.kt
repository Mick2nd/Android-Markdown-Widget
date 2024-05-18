package ch.tiim.markdown_widget.di

import android.content.Context
import dagger.Module
import dagger.Provides

@Module
class ContextModule(val context: Context) {
    @Provides
    fun provideContext() : Context = context
}

/*
@Module
class ContextModule {
    @Provides
    fun provideContext(context: Context) : Context = context
}
*/
