package ch.tiim.markdown_widget.di

import dagger.Module

@Module(includes = [ ContextModule.Bindings::class ])
class ContextModule {

    @Module
    interface Bindings {
    }
}
