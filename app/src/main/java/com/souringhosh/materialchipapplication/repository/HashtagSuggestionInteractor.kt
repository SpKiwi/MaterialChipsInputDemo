package com.souringhosh.materialchipapplication.repository

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

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
                .flatMapLatest { searchString ->
                    flow<List<Suggestion>> {
                        emit(suggestionRepository.getSuggestions())
                    }
                }
                .map { searchResult ->
                    val suggestions = searchResult
                            .filter {
                                !currentHashtags.contains(it.value)
                            }
                    State.Loaded(suggestions)
                }
        val loadingFlow: Flow<State> = loadingChannel
                .asFlow()

        return flowOf(loadingFlow, filteredSuggestionsFlow).flattenMerge()
    }

    @Synchronized
    fun getSuggestions(currentHashtags: List<String>, search: String?) {
        val fixedSearch = search ?: ""
        loadingChannel.offer(State.Loading)
        suggestionChannel.offer(fixedSearch)
    }

    companion object {
        private const val SEARCH_DEBOUNCE = 1_000L
    }

    sealed class State {
        object Loading : State()
        data class Loaded(val suggestions: List<Suggestion>) : State()
    }

}