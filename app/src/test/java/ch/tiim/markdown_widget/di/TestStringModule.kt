package ch.tiim.markdown_widget.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import javax.inject.Named

@Module
@DisableInstallInCheck
class TestStringModule {

    @Provides
    @Named("SUBFOLDER")
    fun provideString() : String = "Dummy"
}
