package ch.tiim.markdown_widget.di

import dagger.Module
import dagger.Provides
import kotlin.reflect.KProperty

/**
 * This wrapper wraps an arbitrary type
 * Goal is to break the di cycle for certain types like Context or String
 * Use this in the appropriate type's module file, here [StringModule], [ContextModule]
 */
class Wrapper<T>(private val injected: T) {

    /**
     * Permits property access syntax for wrapped type
     */
    operator fun getValue(value: Nothing?, any: KProperty<*>) : T {
        return injected
    }
}

/**
 * This helps to provide an implementation for the wrapped type
 */
@Module
abstract class BaseModule<T> {
    @Provides
    fun provideWrapper(wrapped: T) : Wrapper<T> = Wrapper(wrapped)
}
