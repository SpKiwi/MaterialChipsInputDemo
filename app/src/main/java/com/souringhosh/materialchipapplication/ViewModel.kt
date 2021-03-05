package com.souringhosh.materialchipapplication

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.souringhosh.materialchipapplication.repository.Suggestion
import com.souringhosh.materialchipapplication.repository.HashtagSuggestionInteractor
import com.souringhosh.materialchipapplication.utils.events.SingleEventFlag
import com.souringhosh.materialchipapplication.utils.extensions.exhaustive
import com.souringhosh.materialchipapplication.utils.helpers.startWith
import com.souringhosh.materialchipapplication.utils.ui.adapter.ListItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

//interface ViewModel {
//    val hashtags: LiveData<List<Hashtag>>
//    val error: LiveData<HashtagFailureReason?>
//    val suggestions: LiveData<List<Suggestion>>
//    val isSuggestionsLoading: LiveData<Boolean>
//
//    fun selectActiveHashtag(position: Int)
//    fun selectSuggestion(position: Int)
//
//    fun deleteHashtag(position: Int)
//    fun deleteFromHashtag(position: Int)
//    fun editHashtag(hashtagPosition: Int, before: String, after: String)
//}

@ExperimentalCoroutinesApi
@FlowPreview
class ViewModelImpl(
        private val suggestionInteractor: HashtagSuggestionInteractor
) : /*ViewModel, */CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /**
     * For a more efficient usage of diff util
     **/
    private val lastHashtagId: AtomicLong = AtomicLong(0)
    private fun generateId(): Long {
        return lastHashtagId.getAndIncrement()
    }

    private val hashtagsMutable: MutableLiveData<List<Hashtag>> = MutableLiveData<List<Hashtag>>().startWith(
            listOf(Hashtag(generateId(), "", Hashtag.State.LAST))
    )
    val hashtags: LiveData<List<Hashtag>> get() = hashtagsMutable

    private val errorMutable: MutableLiveData<HashtagFailureReason?> = MutableLiveData()
    val error: LiveData<HashtagFailureReason?> get() = errorMutable

    private val suggestionsMutable: MutableLiveData<List<Suggestion>> = MutableLiveData<List<Suggestion>>().startWith(emptyList())
    val suggestions: LiveData<List<Suggestion>> get() = suggestionsMutable

    private val isSuggestionsLoadingMutable: MutableLiveData<Boolean> = MutableLiveData<Boolean>().startWith(true)
    val isSuggestionsLoading: LiveData<Boolean> get() = isSuggestionsLoadingMutable

    private var currentHashtagPosition: Int = 0

    private val currentHashtags: List<Hashtag> get() = hashtagsMutable.value ?: emptyList()
    private val currentSuggestions: List<Suggestion> get() = suggestionsMutable.value ?: emptyList()

    private fun getHashtagStringList(): List<String> = currentHashtags.map { it.text }

    private fun getHashtagStringList(currentHashtags: List<Hashtag>): List<String> = currentHashtags.map { it.text }

    fun start() {
        launch {
            suggestionInteractor.observeSuggestions()
                    .collect {
                        suggestionsMutable.postValue(it)
                    }
        }
        launch {
            suggestionInteractor.observeInappropriateWords()
                    .collect {
                        println(it)
                    }
        }
        suggestionInteractor.getSuggestions(emptyList(), null, null)
    }

    fun selectActiveHashtag(position: Int) {
        val previousHashtagPosition = currentHashtagPosition
        if (previousHashtagPosition != position) {
            currentHashtagPosition = position
            val currentHashtag = currentHashtags[position]
            suggestionInteractor.getSuggestions(getHashtagStringList(), currentHashtag.text, currentHashtag.id)

            val newHashtags = currentHashtags.toMutableList()
                    .mapIndexed { index, hashtag ->
                        val newState = when (index) {
                            currentHashtags.lastIndex -> Hashtag.State.LAST
                            position -> Hashtag.State.EDIT
                            else -> Hashtag.State.READY
                        }
                        hashtag.copy(text = hashtag.text, state = newState)
                    }
            hashtagsMutable.postValue(newHashtags)
        }
    }

    fun selectSuggestion(position: Int) {
        val selectedSuggestionText = currentSuggestions.getOrNull(position)?.value ?: return
        val newHashtags: MutableList<Hashtag> = currentHashtags
                .mapIndexed { index, hashtag ->
                    val newText = if (index == currentHashtagPosition) selectedSuggestionText else hashtag.text
                    hashtag.copy(text = newText, state = Hashtag.State.READY)
                }
                .toMutableList()
                .apply {
                    val lastIndex = currentHashtags.lastIndex
                    if (currentHashtagPosition == lastIndex) {
                        add(Hashtag(generateId(), "#", Hashtag.State.LAST, shouldGainFocus = SingleEventFlag(true)))
                        currentHashtagPosition++
                    } else {
                        currentHashtagPosition = lastIndex
                        set(lastIndex, currentHashtags[lastIndex].copy(shouldGainFocus = SingleEventFlag(true)))
                    }
                }

        val newHashtag = newHashtags[currentHashtagPosition]
        suggestionInteractor.getSuggestions(getHashtagStringList(), newHashtag.text, newHashtag.id)
        hashtagsMutable.postValue(newHashtags)
    }

    fun deleteHashtag(position: Int) {
        if (position < currentHashtagPosition) {
            currentHashtagPosition--
        }

        val newHashtags = currentHashtags
                .toMutableList()
                .apply { removeAt(position) }
        hashtagsMutable.postValue(newHashtags)
    }

    fun deleteFromHashtag(position: Int) {
        val priorElementPosition = position - 1
        if (currentHashtags.getOrNull(priorElementPosition) == null) {
            return
        }

        currentHashtagPosition = priorElementPosition
        val newHashtags = currentHashtags
                .toMutableList()
                .apply {
                    removeAt(priorElementPosition)
                }
        hashtagsMutable.postValue(newHashtags)
    }

    fun editHashtag(hashtagPosition: Int, before: String, after: String) {
        currentHashtagPosition = hashtagPosition
//        val _currentHashtag = currentHashtags[hashtagPosition] // todo remove this
//        suggestionInteractor.checkIsAppropriate(_currentHashtag.id, _currentHashtag.text) // todo remove this

        val newHashtags: List<Hashtag>
        val input = Input(before = before, after = after)

        when (val validationResult = validateText(input)) {
            is HashtagInputValidation.Success -> {
                val currentHashtag = currentHashtags[hashtagPosition]
                if (after.isEmpty() && currentHashtag.state == Hashtag.State.EDIT) {
                    /* Delete this element */
                    newHashtags = currentHashtags
                            .toMutableList()
                            .apply {
                                removeAt(hashtagPosition)
                            }
                    suggestionInteractor.getSuggestions(getHashtagStringList(newHashtags), newHashtags.last().text, newHashtags.last().id)
                } else {
                    val newHashtagState = if (hashtagPosition == currentHashtags.lastIndex) Hashtag.State.LAST else Hashtag.State.EDIT
                    val newHashtag = currentHashtags[hashtagPosition].copy(text = after, state = newHashtagState)
                    newHashtags = currentHashtags
                            .toMutableList()
                            .apply {
                                set(hashtagPosition, newHashtag)
                            }
                    suggestionInteractor.getSuggestions(getHashtagStringList(newHashtags), newHashtag.text, newHashtag.id)
                }
            }
            is HashtagInputValidation.HashtagFinished -> {
                val correctedHashtag = validationResult.correctedHashtag
                val newHashtag = currentHashtags[hashtagPosition].copy(
                        text = validationResult.correctedHashtag,
                        state = Hashtag.State.READY,
                        shouldCorrectSpelling = SingleEventFlag(correctedHashtag != after)
                )
                newHashtags = currentHashtags
                        .toMutableList()
                        .apply {
                            set(hashtagPosition, newHashtag)
                            if (removeTrailingHashtag(last().text).isBlank()) {
                                set(lastIndex, last().copy(shouldGainFocus = SingleEventFlag(true)))
                            } else {
                                add(Hashtag(generateId(), "#", Hashtag.State.LAST, shouldGainFocus = SingleEventFlag(true)))
                            }
                        }
                suggestionInteractor.getSuggestions(getHashtagStringList(newHashtags), null, null)
            }
            is HashtagInputValidation.Failure -> {
                val correctedHashtag = validationResult.correctedHashtag
                val newHashtagState = if (hashtagPosition == currentHashtags.lastIndex) Hashtag.State.LAST else Hashtag.State.EDIT
                val newHashtag = currentHashtags[hashtagPosition].copy(
                        text = validationResult.correctedHashtag,
                        state = newHashtagState,
                        shouldCorrectSpelling = SingleEventFlag(correctedHashtag != after)
                )
                newHashtags = currentHashtags
                        .toMutableList()
                        .apply {
                            set(hashtagPosition, newHashtag)
                        }
                errorMutable.postValue(validationResult.reason)
            }
        }.exhaustive
        hashtagsMutable.postValue(newHashtags)
    }

    fun getTitleInfo(): List<String> = getHashtagStringList().map { removeTrailingHashtag(it) }

    private data class Input(
            val before: String,
            val after: String
    ) {
        fun isStartNewHashtag(): Boolean = after.length - before.length == 1 && hashtagEndChars.contains(after.last())
        fun isSingleHashtagSymbol(): Boolean = after.length == 1 && after[0] == '#'
    }

    private fun validateText(input: Input): HashtagInputValidation {
        if (input.isSingleHashtagSymbol()) {
            return HashtagInputValidation.Success
        }

        val formattedInput = removeTrailingHashtag(input.after)
        if (formattedInput.isEmpty()) {
            return HashtagInputValidation.Success
        }

        if (input.isStartNewHashtag()) {
            val fixedStringBuilder = StringBuilder()
            input.after.forEachIndexed { index, char ->
                if (isCharValid(index, char))
                    fixedStringBuilder.append(char)
            }
            if (fixedStringBuilder.length > MIN_HASHTAG_LENGTH) {
                if (!fixedStringBuilder.startsWith("#")) {
                    fixedStringBuilder.insert(0, "#")
                }
                return HashtagInputValidation.HashtagFinished(fixedStringBuilder.toString())
            }
        }

        input.after.withIndex()
                .find { !isCharValid(it.index, it.value) }
                ?.let {
                    return HashtagInputValidation.Failure(
                            HashtagFailureReason.WRONG_SYMBOL,
                            input.before
                    )
                }

        if (formattedInput.length > MAX_HASHTAG_LENGTH) {
            return HashtagInputValidation.Failure(
                    HashtagFailureReason.MAX_SIZE_EXCEEDED,
                    input.after.take(MAX_HASHTAG_LENGTH)
            )
        }

        return HashtagInputValidation.Success
    }

    private val charValidation: (Char) -> Boolean = { it.isLetterOrDigit() || it == '_' || it == '-' }
    private fun isCharValid(index: Int, char: Char): Boolean {
        return if (index == 0) {
            charValidation(char) || char == '#'
        } else {
            charValidation(char)
        }
    }

    private fun removeTrailingHashtag(hashtagText: String): String {
        return if (hashtagText.startsWith('#')) {
            hashtagText.replaceFirst("#", "")
        } else {
            hashtagText
        }
    }

    private sealed class HashtagInputValidation {
        object Success : HashtagInputValidation()
        data class HashtagFinished(val correctedHashtag: String) : HashtagInputValidation()
        data class Failure(
                val reason: HashtagFailureReason,
                val correctedHashtag: String
        ) : HashtagInputValidation()
    }

    companion object {
        /**
         * Hashtag length is counted without the '#' symbol
         **/
        private const val MAX_HASHTAG_LENGTH = 50
        private const val MIN_HASHTAG_LENGTH = 1
        private const val CARRIAGE_RETURN = 13
        private const val LINE_FEED = 10
        private val hashtagEndChars: List<Char> = listOf(
                CARRIAGE_RETURN.toChar(),
                LINE_FEED.toChar(),
                ' ',
                '#'
        )
    }
}

enum class HashtagFailureReason {
    MAX_SIZE_EXCEEDED,
    WRONG_SYMBOL,
    INAPPROPRIATE_LANGUAGE
}

data class Hashtag(
        override val id: Long,
        val text: String,
        val state: State,
        val shouldCorrectSpelling: SingleEventFlag = SingleEventFlag(),
        val shouldGainFocus: SingleEventFlag = SingleEventFlag()
) : ListItem {

    enum class State {
        /**
         * Hashtag which the user is currently entering and is not last
         **/
        EDIT,

        /**
         * Hashtag which is finished and not being edited
         **/
        READY, // todo add substates //

        /**
         * Hashtag that is selected and can be deleted by pressing back again
         * TODO: not yet implemented
         **/
        SELECTED,

        /**
         * Last hashtag always has to keep this state
         **/
        LAST
    }
}