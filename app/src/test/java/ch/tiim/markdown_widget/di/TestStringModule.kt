package ch.tiim.markdown_widget.di

import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class TestStringModule : BaseModule<String>() {

    @Provides
    @Named("SUBFOLDER")
    fun provideString() : String = "Dummy"
}
