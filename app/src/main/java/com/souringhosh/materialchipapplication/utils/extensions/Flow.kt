package com.souringhosh.materialchipapplication.utils.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@FlowPreview
fun <T, K> Flow<T>.groupBy(keySelector: suspend (T) -> K) : Flow<GroupedFlow<K, T>> =
        FlowGroupBy(this, keySelector, { it })

interface GroupedFlow<K, V> : Flow<V> {
    /**
     * The key of the flow.
     */
    val key : K
}

/**
 * Groups transformed values of the source flow based on a key selector
 * function.
 */
@FlowPreview
internal class FlowGroupBy<T, K, V>(
        private val source: Flow<T>,
        private val keySelector: suspend (T) -> K,
        private val valueSelector: suspend (T) -> V
) : AbstractFlow<GroupedFlow<K, V>>() {

    override suspend fun collectSafely(collector: FlowCollector<GroupedFlow<K, V>>) {
        val map = ConcurrentHashMap<K, FlowGroup<K, V>>()

        val mainStopped = AtomicBoolean()

        try {
            source.collect {
                val k = keySelector(it)

                var group = map[k]

                if (group != null) {
                    group.next(valueSelector(it))
                } else {
                    if (!mainStopped.get()) {
                        group = FlowGroup(k, map)
                        map.put(k, group)

                        try {
                            collector.emit(group)
                        } catch (ex: CancellationException) {
                            mainStopped.set(true)
                            if (map.size == 0) {
                                throw CancellationException()
                            }
                        }

                        group.next(valueSelector(it))
                    } else {
                        if (map.size == 0) {
                            throw CancellationException()
                        }
                    }
                }
            }
            for (group in map.values) {
                group.complete()
            }
        } catch (ex: Throwable) {
            for (group in map.values) {
                group.error(ex)
            }
        }
    }

    class FlowGroup<K, V>(
            override val key: K,
            private val map : ConcurrentMap<K, FlowGroup<K, V>>
    ) : AbstractFlow<V>(), GroupedFlow<K, V> {

        @Suppress("UNCHECKED_CAST")
        private var value: V = null as V
        @Volatile
        private var hasValue: Boolean = false

        private var error: Throwable? = null
        @Volatile
        private var done: Boolean = false

        @Volatile
        private var cancelled: Boolean = false

        private val consumerReady = Resumable()

        private val valueReady = Resumable()

        private val once = AtomicBoolean()

        override suspend fun collectSafely(collector: FlowCollector<V>) {
            if (!once.compareAndSet(false, true)) {
                throw IllegalStateException("A GroupedFlow can only be collected at most once.")
            }

            consumerReady.resume()

            while (true) {
                val d = done
                val has = hasValue

                if (d && !has) {
                    val ex = error
                    if (ex != null) {
                        throw ex
                    }
                    break
                }

                if (has) {
                    val v = value
                    @Suppress("UNCHECKED_CAST")
                    value = null as V
                    hasValue = false

                    try {
                        collector.emit(v)
                    } catch (ex: Throwable) {
                        map.remove(this.key)
                        cancelled = true
                        consumerReady.resume()
                        throw ex
                    }

                    consumerReady.resume()
                    continue
                }

                valueReady.await()
            }
        }

        suspend fun next(value: V) {
            if (!cancelled) {
                consumerReady.await()
                this.value = value
                this.hasValue = true
                valueReady.resume()
            }
        }

        fun error(ex: Throwable) {
            error = ex
            done = true
            valueReady.resume()
        }

        fun complete() {
            done = true
            valueReady.resume()
        }
    }
}

open class Resumable : AtomicReference<Continuation<Any>>() {

    private companion object {
        val READY = ReadyContinuation()
        val VALUE = Object()
    }

    /**
     * Await the resumption of this Resumable, suspending the
     * current coroutine if necessary.
     * Only one thread can call this method.
     */
    suspend fun await() {
        suspendCancellableCoroutine<Any> {
            while (true) {
                val current = get()
                if (current == READY) {
                    it.resumeWith(Result.success(VALUE))
                    break
                }
                if (current != null) {
                    throw IllegalStateException("Only one thread can await a Resumable")
                }
                if (compareAndSet(current, it)) {
                    break
                }
            }
        }
        getAndSet(null)
    }

    /**
     * Resume this Resumable, resuming any currently suspended
     * [await] callers.
     * This method can be called by any number of threads.
     */
    fun resume() {
        if (get() == READY) {
            return
        }
        getAndSet(READY)?.resumeWith(Result.success(VALUE))
    }

    /**
     * Represents a stateless indicator if the continuation is already
     * ready for resumption, thus no need to get suspended.
     */
    private class ReadyContinuation : Continuation<Any> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Any>) {
            // The existence already indicates resumption
        }
    }
}