package ch.tiim.markdown_widget.di

import android.net.Uri
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import javax.inject.Scope

@Scope
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class ActivityScope

/**
 * Empty at the moment
 */
@Module
@DisableInstallInCheck
class ActivityModule() {
}
