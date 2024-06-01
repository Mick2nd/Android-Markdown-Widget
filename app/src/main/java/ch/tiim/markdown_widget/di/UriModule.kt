package ch.tiim.markdown_widget.di

import android.content.Context
import android.net.Uri
import ch.tiim.markdown_widget.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import java.io.File
import javax.inject.Named

@Module
@DisableInstallInCheck
class UriModule {

    @Provides
    @Named("INTERNAL")
    fun provideUri1(context: Context) : Uri {
        val file = File(context.filesDir, "public/userstyle.css")
        return Uri.fromFile(file)
    }

    @Provides
    @Named("EXTERNAL")
    fun provideUri2(context: Context, @Named("SUBFOLDER") subFolder: String) : Uri {
        val file = File(context.getExternalFilesDir(subFolder), "userstyle.css")
        return Uri.fromFile(file)
    }

    @Provides
    @Named("GLOBAL")
    fun provideUri3(prefs: Preferences) : Uri = prefs.userDocumentUriOf("userstyle.css")

    @Provides
    @Named("USER-FOLDER")
    fun provideUri4(prefs: Preferences) : Uri? = prefs.userFolderUri
}
