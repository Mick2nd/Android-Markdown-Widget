package ch.tiim.markdown_widget.di

import android.os.Environment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.migration.DisableInstallInCheck
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
// @DisableInstallInCheck
class StringModule {

    @Provides
    @Named("SUBFOLDER")
    fun provideString() : String = Environment.DIRECTORY_DOCUMENTS
}
