package ch.tiim.markdown_widget.di

import ch.tiim.markdown_widget.DiSingletonTest
import ch.tiim.markdown_widget.ExternalStoragePathHandler
import ch.tiim.markdown_widget.FileServices
import ch.tiim.markdown_widget.Preferences
import ch.tiim.markdown_widget.StoragePermissionChecker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Currently not used.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TestEntryPoint {
    fun inject(test: DiSingletonTest)

    @Singleton
    fun preferences() : Preferences

    @Singleton
    fun externalStoragePathHandler() : ExternalStoragePathHandler

    @Singleton
    fun storagePermissionChecker() : StoragePermissionChecker

    @Singleton
    fun fileChecker() : FileServices
}
