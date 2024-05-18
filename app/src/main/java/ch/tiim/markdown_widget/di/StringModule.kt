package ch.tiim.markdown_widget.di

import dagger.Module
import dagger.Provides

@Module
class StringModule(val string: String) {
    @Provides
    fun provideString() : String = string
}

/*
@Module
class StringModule {
    @Provides
    fun provideString(string: String) : String = string
}
*/
