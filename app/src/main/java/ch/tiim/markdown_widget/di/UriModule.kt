package ch.tiim.markdown_widget.di

import android.content.Context
import android.net.Uri
import ch.tiim.markdown_widget.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.migration.DisableInstallInCheck
import java.io.File
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
class UriModule {

    @Provides
    @Named("INTERNAL")
    fun provideUri1(@ApplicationContext context: Context) : Uri {
        val file = File(context.filesDir, "public/userstyle.css")
        return Uri.fromFile(file)
    }

    @Provides
    @Named("EXTERNAL")
    fun provideUri2(@ApplicationContext context: Context, @Named("SUBFOLDER") subFolder: String) : Uri {
        val file = File(context.getExternalFilesDir(subFolder), "userstyle.css")
        return Uri.fromFile(file)
    }

    @Provides
    @Named("GLOBAL")
    fun provideUri3(prefs: Preferences) : Uri =
        try { prefs.userDocumentUriOf("userstyle.css") }
        catch(err: Exception) { Uri.parse("") }

    @Provides
    @Named("USER-FOLDER")
    fun provideUri4(prefs: Preferences) : Uri? = prefs.userFolderUri
}
