package com.souringhosh.materialchipapplication.repository

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

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
                .flatMapLatest { _ ->
                    flow<State> {
                        if (Random.nextBoolean())
                            throw RuntimeException()
                        
                        val suggestions = suggestionRepository.getSuggestions()
                                .filter {
                                    !currentHashtags.contains(it.value)
                                }

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