package ch.tiim.markdown_widget.di

import ch.tiim.markdown_widget.StoragePermissionChecker
import ch.tiim.markdown_widget.StoragePermissionCheckerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Singleton

/**
 * Empty at the moment
 */
@Module(includes = [])
@InstallIn(ActivityComponent::class)
class ActivityModule() {

    @Module
    @InstallIn(ActivityComponent::class)
    interface Bindings {
    }
}
