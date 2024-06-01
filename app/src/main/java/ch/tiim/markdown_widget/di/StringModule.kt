package ch.tiim.markdown_widget.di

import android.os.Environment
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import javax.inject.Named

@Module
@DisableInstallInCheck
class StringModule {

    @Provides
    @Named("SUBFOLDER")
    fun provideString() : String = Environment.DIRECTORY_DOCUMENTS
}
