package ch.tiim.markdown_widget.di

import android.content.Context
import dagger.Module

@Module(includes = [ ContextModule.Bindings::class ])
class ContextModule : BaseModule<Context>() {

    @Module
    interface Bindings {
    }
}
