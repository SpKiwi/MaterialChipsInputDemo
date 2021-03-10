package com.souringhosh.materialchipapplication.repository

import com.souringhosh.materialchipapplication.utils.extensions.groupBy
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
@FlowPreview
class HashtagSuggestionInteractor(
        private val suggestionRepository: SuggestionRepository
) {
    /**
     * This channel is for local hashtag id and the corresponding hashtag value
     **/
    private val suggestionChannel = BroadcastChannel<Pair<Long, String>>(Channel.CONFLATED)
    private val inappropriateWordsChannel = BroadcastChannel<Set<String>>(Channel.CONFLATED)

    private var currentHashtags: List<String> = emptyList()
    private var lastSearchId: Long = -1

    fun observeInappropriateWords(): Flow<Set<String>> {
        return inappropriateWordsChannel
                .asFlow()
                .onStart { emit(emptySet()) }
                .runningReduce { accumulator, value ->
                    accumulator
                            .toMutableSet()
                            .apply { addAll(value) }
                }
    }

    fun observeSuggestions(): Flow<List<Suggestion>> {
        return suggestionChannel
            .asFlow()
            .groupBy { (id, _) -> id }
            .flatMapMerge { groupedFlow ->
                groupedFlow
                    .debounce(SEARCH_DEBOUNCE)
                    .flatMapLatest { (hashtagId, hashtagText) ->
                        val uniqueSearch = flow {
                            val searchResult = suggestionRepository.getSuggestions(hashtagText)
                            if (!searchResult.isResponseValid) {
                                inappropriateWordsChannel.offer(setOf(hashtagText))
                            }

                            if (hashtagId == lastSearchId) {
                                val suggestions = searchResult
                                   .items
                                   .asSequence()
                                   .map { it.value }
                                   .filter { !currentHashtags.contains(it) && it.isNotEmpty() }
                                   .take(SUGGESTION_LIST_SIZE)
                                   .map { Suggestion("#$it") }
                                   .toList()

                                emit(suggestions)
                            }
                        }

                        uniqueSearch.retry {
                            delay(RETRY_SEARCH_AFTER)
                            true
                        }
                    }
            }
    }

    @Synchronized
    fun getSuggestions(
            currentHashtags: List<String>,
            search: String?,
            localId: Long?
    ) {
        val currentSearch: String = search?.removePrefix("#") ?: ""
        val currentHashtagId: Long = localId ?: -1

        this.currentHashtags = currentHashtags
        this.lastSearchId = currentHashtagId

        suggestionChannel.offer(currentHashtagId to currentSearch)
    }

    companion object {
        private const val SEARCH_DEBOUNCE = 1_000L
        private const val SUGGESTION_LIST_SIZE = 10
        private const val RETRY_SEARCH_AFTER = 1_000L
    }

}