package ch.tiim.markdown_widget.di

import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck

@Module(includes = [ ContextModule.Bindings::class ])
@DisableInstallInCheck
class ContextModule {

    @Module
    @DisableInstallInCheck
    interface Bindings {
    }
}
