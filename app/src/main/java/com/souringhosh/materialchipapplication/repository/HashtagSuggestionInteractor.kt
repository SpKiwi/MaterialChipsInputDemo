package com.souringhosh.materialchipapplication.repository

import android.util.Log
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
    private val suggestionChannel = BroadcastChannel<String>(Channel.CONFLATED)
    private val loadingChannel = BroadcastChannel<State>(Channel.CONFLATED)
    private var currentHashtags: List<String> = emptyList()

    fun observeSuggestions(): Flow<State> {
        val filteredSuggestionsFlow: Flow<State> = suggestionChannel
                .asFlow()
                .debounce(SEARCH_DEBOUNCE)
                .flatMapLatest { suggestion ->
                    flow<State> {
                        val suggestions = suggestionRepository.getSuggestions(suggestion)
                                .items
                                .asSequence()
                                .map { it.value }
                                .filter { !currentHashtags.contains(it) }
                                .take(SUGGESTION_LIST_SIZE)
                                .map { Suggestion("#$it") }
                                .toList()

                        emit(State.Loaded(suggestions))
                    }
                            .catch {
                                emit(State.Loaded(emptyList()))
                            }
                }
        val loadingFlow: Flow<State> = loadingChannel
                .asFlow()

        return flowOf(loadingFlow, filteredSuggestionsFlow).flattenMerge()
    }

    @Synchronized
    fun getSuggestions(currentHashtags: List<String>, search: String?) {
        val currentSearch = search ?: ""
        loadingChannel.offer(State.Loading)
        suggestionChannel.offer(currentSearch)
    }

    companion object {
        private const val SEARCH_DEBOUNCE = 1_000L
        private const val SUGGESTION_LIST_SIZE = 10
    }

    sealed class State {
        object Loading : State()
        data class Loaded(val suggestions: List<Suggestion>) : State()
    }

}