package com.souringhosh.materialchipapplication.repository

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SuggestionInteractor(
        private val suggestionRepository: SuggestionRepository
) {
    private val suggestionSubject: PublishSubject<Unit> = PublishSubject.create()
    private val loadingSubject: PublishSubject<State> = PublishSubject.create()
    private var filter: List<String> = emptyList()

    fun observeSuggestions(): Observable<State> {
        val some: Observable<State> = suggestionSubject.switchMap {
            suggestionRepository
                    .getSuggestions() // todo on error wait and retry
                    .toObservable()
        }
                .map { suggestions ->
                    State.Loaded(
                            suggestions.filter { suggestion ->
                                filter.contains(suggestion.value)
                            }
                    )
                }

        return Observable.merge(
                some,
                loadingSubject
        )
    }

    @Synchronized
    fun getSuggestions(filter: List<String>, search: String?) {
        this.filter = filter
        loadingSubject.onNext(State.Loading)
        suggestionSubject.onNext(Unit)
    }

    sealed class State {
        object Loading : State()
        data class Loaded(val suggestions: List<Suggestion>) : State()
    }

}