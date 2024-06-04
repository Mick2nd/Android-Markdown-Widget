package ch.tiim.markdown_widget.di

import ch.tiim.markdown_widget.MarkdownRenderer
import ch.tiim.markdown_widget.UpdateService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@EntryPoint
@InstallIn(SingletonComponent::class)
interface CustomEntryPoint {
    fun inject(renderer: MarkdownRenderer)
}
