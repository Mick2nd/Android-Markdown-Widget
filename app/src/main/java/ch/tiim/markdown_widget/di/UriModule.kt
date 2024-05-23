package ch.tiim.markdown_widget.di

import android.content.Context
import android.net.Uri
import android.os.Environment
import dagger.Module
import dagger.Provides
import java.io.File
import javax.inject.Named

@Module
class UriModule {

    @Provides
    @Named("INTERNAL")
    fun provideUri1(context: Context) : Uri {
        val file = File(context.filesDir, "public/userstyle.css")
        return Uri.fromFile(file)
    }

    @Provides
    @Named("EXTERNAL")
    fun provideUri2(context: Context) : Uri {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "userstyle.css")
        return Uri.fromFile(file)
    }
}
