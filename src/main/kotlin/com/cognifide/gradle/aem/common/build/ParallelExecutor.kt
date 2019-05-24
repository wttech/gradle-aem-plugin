package com.cognifide.gradle.aem.common.build

import com.cognifide.gradle.aem.AemExtension
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*

@UseExperimental(ObsoleteCoroutinesApi::class)
class ParallelExecutor(aem: AemExtension) {

    val enabled = aem.props.boolean("parallel") ?: true

    fun <A, B : Any> map(iterable: Iterable<A>, mapper: (A) -> B): Collection<B> {
        return map(iterable, { true }, mapper)
    }

    fun <A, B : Any> map(iterable: Iterable<A>, filter: (A) -> Boolean, mapper: (A) -> B): List<B> {
        return map(Dispatchers.Default, iterable) { it.takeIf(filter)?.let(mapper) }
    }

    fun <A, B : Any> pool(threads: Int, name: String, iterable: Iterable<A>, mapper: (A) -> B): List<B> {
        return map(newFixedThreadPoolContext(threads, name), iterable, mapper)
    }

    private fun <A, B : Any> map(context: CoroutineContext, iterable: Iterable<A>, mapper: (A) -> B?): List<B> {
        if (!enabled) {
            return iterable.mapNotNull(mapper)
        }

        return runBlocking(context) {
            iterable.map { value -> async { value?.let(mapper) } }.mapNotNull { it.await() }
        }
    }

    fun <A> with(iterable: Iterable<A>, callback: A.() -> Unit) {
        map(iterable) { it.apply(callback); Unit }
    }
}