package com.souringhosh.materialchipapplication

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.souringhosh.materialchipapplication.repository.Suggestion
import com.souringhosh.materialchipapplication.repository.HashtagSuggestionInteractor
import com.souringhosh.materialchipapplication.repository.HashtagSuggestionRepository
import com.souringhosh.materialchipapplication.utils.events.SingleEventFlag
import com.souringhosh.materialchipapplication.utils.extensions.asFlow
import com.souringhosh.materialchipapplication.utils.extensions.exhaustive
import com.souringhosh.materialchipapplication.utils.helpers.startWith
import com.souringhosh.materialchipapplication.utils.ui.adapter.ListItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
@FlowPreview
class ViewModelImpl(
        private val hashtagSuggestionRepository: HashtagSuggestionRepository,
        private val suggestionInteractor: HashtagSuggestionInteractor
) : CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /**
     * For a more efficient usage of diff util
     **/
    private val lastHashtagId: AtomicLong = AtomicLong(0)
    private fun generateId(): Long {
        return lastHashtagId.getAndIncrement()
    }

    private val hashtagsMutable: MutableLiveData<List<Hashtag>> = MutableLiveData<List<Hashtag>>().startWith(
            listOf(Hashtag(
                    id = generateId(),
                    text = "",
                    state = Hashtag.State.LAST,
                    inputType = HashtagInputType.CUSTOM
            ))
    )
    val hashtags: LiveData<List<Hashtag>> get() = hashtagsMutable

    private val typingErrorMutable: MutableLiveData<HashtagFailureReason?> = MutableLiveData<HashtagFailureReason?>().startWith(null)
    private val errorMutable: MutableLiveData<List<HashtagFailureReason>> = MutableLiveData()
    val error: LiveData<List<HashtagFailureReason>> get() = errorMutable

    private val suggestionsMutable: MutableLiveData<List<Suggestion>> = MutableLiveData<List<Suggestion>>().startWith(emptyList())
    val suggestions: LiveData<List<Suggestion>> get() = suggestionsMutable

    private val isSuggestionsLoadingMutable: MutableLiveData<Boolean> = MutableLiveData<Boolean>().startWith(true)

    private var currentHashtagPosition: Int = 0
    private val maxHashtagCount = 3

    private val currentHashtags: List<Hashtag> get() = hashtagsMutable.value ?: emptyList()
    private val currentSuggestions: List<Suggestion> get() = suggestionsMutable.value ?: emptyList()

    private lateinit var getPreviousHashtagsJob: Job

    fun start() {
        launch {
            hashtags.asFlow()
                    .combine(suggestionInteractor.observeSuggestions()) { hashtags, suggestions ->
                        val hashtagsText = hashtags.map { hashtag -> hashtag.text.removePrefix("#") }
                        suggestions
                                .asSequence()
                                .filter { suggestion ->
                                    !hashtagsText.contains(suggestion) && suggestion.isNotEmpty()
                                }
                                .take(SUGGESTION_LIST_SIZE)
                                .map { Suggestion("#$it") }
                                .toList()
                    }
                    .collect {
                        suggestionsMutable.postValue(it)
                    }
        }
        launch {
            /**
             * All input-related errors will be here, but for now we only show inappropriate word-connected errors
             **/
            hashtags.asFlow()
                    .combine(suggestionInteractor.observeInappropriateWords()) { hashtags, inappropriateWords ->
                        val containsInappropriateWords = hashtags.map { it.text }
                                .any { hashtag ->
                                    inappropriateWords.any { word ->
                                        hashtag.contains(word)
                                    }
                                }
                        containsInappropriateWords to hashtags.size
                    }
                    .combine(typingErrorMutable.asFlow()) { (containsInappropriateWords, hashtagsSize), typingError ->
                        containsInappropriateWords to hashtagsSize
                        /**
                         * All typing errors can be handled here if needed
                         **/
                    }
                    .collect { (containsInappropriateWords, hashtagsSize) ->
                        val errors = mutableListOf<HashtagFailureReason>().apply {
                            if (containsInappropriateWords) {
                                add(HashtagFailureReason.INAPPROPRIATE_LANGUAGE)
                            }
                            if (hashtagsSize > maxHashtagCount) {
                                add(HashtagFailureReason.MAX_HASHTAGS_EXCEEDED)
                            }
                        }
                        errorMutable.postValue(errors)
                    }
        }
        getPreviousHashtagsJob = launch {
            val result = hashtagSuggestionRepository.getHashtagSuggestions()
            val newHashtags = currentHashtags
                    .toMutableList()
                    .apply {
                        addAll(
                                0,
                                result.map {
                                    Hashtag(
                                            id = generateId(),
                                            text = it,
                                            state = Hashtag.State.READY,
                                            inputType = HashtagInputType.DEFAULT
                                    )
                                }
                        )
                    }
            hashtagsMutable.postValue(newHashtags)
        }
        suggestionInteractor.getSuggestions(null, null)
    }

    fun selectActiveHashtag(position: Int) {
        getPreviousHashtagsJob.cancel()
        val previousHashtagPosition = currentHashtagPosition
        if (previousHashtagPosition != position) {
            currentHashtagPosition = position
            val currentHashtag = currentHashtags[position]
            suggestionInteractor.getSuggestions(currentHashtag.text, currentHashtag.id)

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
        getPreviousHashtagsJob.cancel()
        val selectedSuggestionText = currentSuggestions.getOrNull(position)?.value ?: return
        val newHashtags: MutableList<Hashtag> = currentHashtags
                .mapIndexed { index, hashtag ->
                    val newText: String
                    val newInputType: HashtagInputType
                    if (index == currentHashtagPosition) {
                        newText = selectedSuggestionText
                        newInputType = HashtagInputType.SUGGESTED
                    } else {
                        newText = hashtag.text
                        newInputType = hashtag.inputType
                    }
                    hashtag.copy(
                            text = newText,
                            state = Hashtag.State.READY,
                            inputType = newInputType
                    )
                }
                .toMutableList()
                .apply {
                    val lastIndex = currentHashtags.lastIndex
                    if (currentHashtagPosition == lastIndex) {
                        add(Hashtag(
                                id = generateId(),
                                text = "#",
                                state = Hashtag.State.LAST,
                                inputType = HashtagInputType.CUSTOM,
                                shouldGainFocus = SingleEventFlag(true)
                        ))
                        currentHashtagPosition++
                    } else {
                        currentHashtagPosition = lastIndex
                        set(lastIndex, currentHashtags[lastIndex].copy(shouldGainFocus = SingleEventFlag(true)))
                    }
                }

        val newHashtag = newHashtags[currentHashtagPosition]
        suggestionInteractor.getSuggestions(newHashtag.text, newHashtag.id)
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
        getPreviousHashtagsJob.cancel()
        currentHashtagPosition = hashtagPosition
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
                    suggestionInteractor.getSuggestions(newHashtags.last().text, newHashtags.last().id)
                } else {
                    val newHashtagState = if (hashtagPosition == currentHashtags.lastIndex) Hashtag.State.LAST else Hashtag.State.EDIT
                    val newHashtag = currentHashtags[hashtagPosition].copy(
                            text = after,
                            state = newHashtagState,
                            inputType = HashtagInputType.CUSTOM
                    )
                    newHashtags = currentHashtags
                            .toMutableList()
                            .apply {
                                set(hashtagPosition, newHashtag)
                            }
                    suggestionInteractor.getSuggestions(newHashtag.text, newHashtag.id)
                }
            }
            is HashtagInputValidation.HashtagFinished -> {
                val correctedHashtag = validationResult.correctedHashtag
                if (currentHashtags.size > maxHashtagCount) {
                    val newHashtag = currentHashtags[hashtagPosition].copy(
                            text = validationResult.correctedHashtag,
                            inputType = HashtagInputType.CUSTOM,
                            shouldCorrectSpelling = SingleEventFlag(correctedHashtag != after)
                    )
                    newHashtags = currentHashtags
                            .toMutableList()
                            .apply {
                                set(hashtagPosition, newHashtag)
                            }
                } else {
                    val newHashtag = currentHashtags[hashtagPosition].copy(
                            text = validationResult.correctedHashtag,
                            state = Hashtag.State.READY,
                            inputType = HashtagInputType.CUSTOM,
                            shouldCorrectSpelling = SingleEventFlag(correctedHashtag != after)
                    )
                    newHashtags = currentHashtags
                            .toMutableList()
                            .apply {
                                set(hashtagPosition, newHashtag)
                                if (last().text.removePrefix("#").isBlank()) {
                                    set(lastIndex, last().copy(shouldGainFocus = SingleEventFlag(true)))
                                } else {
                                    add(Hashtag(
                                            id = generateId(),
                                            text = "#",
                                            state = Hashtag.State.LAST,
                                            inputType = HashtagInputType.CUSTOM,
                                            shouldGainFocus = SingleEventFlag(true)
                                    ))
                                }
                            }
                }
                currentHashtagPosition = newHashtags.lastIndex
                suggestionInteractor.getSuggestions(null, null)
            }
            is HashtagInputValidation.Failure -> {
                val correctedHashtag = validationResult.correctedHashtag
                val newHashtagState = if (hashtagPosition == currentHashtags.lastIndex) Hashtag.State.LAST else Hashtag.State.EDIT
                val newHashtag = currentHashtags[hashtagPosition].copy(
                        text = validationResult.correctedHashtag,
                        state = newHashtagState,
                        inputType = HashtagInputType.CUSTOM,
                        shouldCorrectSpelling = SingleEventFlag(correctedHashtag != after)
                )
                newHashtags = currentHashtags
                        .toMutableList()
                        .apply {
                            set(hashtagPosition, newHashtag)
                        }
                typingErrorMutable.postValue(validationResult.reason)
            }
        }.exhaustive
        hashtagsMutable.postValue(newHashtags)
    }

    fun getTitleInfo(): List<Hashtag> = currentHashtags.map { hashtag ->
        val formattedText = hashtag.text
                .removePrefix("#")
                .trim()
                .toLowerCase(Locale.US)

        hashtag.copy(text = formattedText)
    }
            .filter { it.text.isNotEmpty() }
            .take(maxHashtagCount)

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

        val formattedInput = input.after.removePrefix("#")
        if (formattedInput.isEmpty()) {
            return HashtagInputValidation.Success
        }

        if (input.isStartNewHashtag()) {
            val fixedStringBuilder = StringBuilder()
            input.after.forEachIndexed { index, char ->
                if (isCharValid(index, char))
                    fixedStringBuilder.append(char)
            }
            if (fixedStringBuilder.removePrefix("#").length >= MIN_HASHTAG_LENGTH) {
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
        private const val SUGGESTION_LIST_SIZE = 10
    }
}

enum class HashtagFailureReason {
    MAX_SIZE_EXCEEDED,
    MAX_HASHTAGS_EXCEEDED,
    WRONG_SYMBOL,
    INAPPROPRIATE_LANGUAGE
}

data class Hashtag(
        override val id: Long,
        val text: String,
        val state: State,
        val inputType: HashtagInputType,
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
        READY,

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