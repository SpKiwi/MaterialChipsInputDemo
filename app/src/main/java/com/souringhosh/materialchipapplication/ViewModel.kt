package com.souringhosh.materialchipapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.souringhosh.materialchipapplication.repository.Suggestion
import com.souringhosh.materialchipapplication.repository.SuggestionInteractor
import com.souringhosh.materialchipapplication.utils.events.SingleEventFlag
import com.souringhosh.materialchipapplication.utils.extensions.exhaustive
import com.souringhosh.materialchipapplication.utils.helpers.DefaultMutableLiveData
import com.souringhosh.materialchipapplication.utils.ui.adapter.ListItem
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.atomic.AtomicLong

interface ViewModel {
    val hashtags: LiveData<List<Hashtag>>
    val error: LiveData<HashtagFailureReason?>
    val suggestions: LiveData<List<Suggestion>>
    val isSuggestionsLoading: LiveData<Boolean>
}

interface ViewModelInteractions {
    fun selectActiveHashtag(position: Int)
    fun selectSuggestion(position: Int)

    fun deleteHashtag(position: Int)
    fun deleteFromHashtag(position: Int)
    fun editHashtag(hashtagPosition: Int, before: String, after: String)

    /**
     * Actions like Done/Enter/Delete
     **/
    fun keyboardAction(position: Int, action: ViewModelImpl.HashtagKeyboardAction)
}

class ViewModelImpl(
        private val suggestionInteractor: SuggestionInteractor
) : ViewModel, ViewModelInteractions {

    /**
     * For a more efficient usage of diff util
     **/
    private val lastHashtagId: AtomicLong = AtomicLong(0)
    private fun generateId(): Long {
        return lastHashtagId.getAndIncrement()
    }

    private val hashtagsMutable: DefaultMutableLiveData<List<Hashtag>> = DefaultMutableLiveData(listOf(Hashtag(generateId(), "", Hashtag.State.LAST)))
    override val hashtags: LiveData<List<Hashtag>> get() = hashtagsMutable

    private val errorMutable: MutableLiveData<HashtagFailureReason?> = MutableLiveData()
    override val error: LiveData<HashtagFailureReason?> get() = errorMutable

    private val suggestionsMutable: DefaultMutableLiveData<List<Suggestion>> = DefaultMutableLiveData(emptyList())
    override val suggestions: LiveData<List<Suggestion>> get() = suggestionsMutable

    private val isSuggestionsLoadingMutable: DefaultMutableLiveData<Boolean> = DefaultMutableLiveData(true)
    override val isSuggestionsLoading: LiveData<Boolean> get() = isSuggestionsLoadingMutable

    private var currentHashtagPosition: Int = 0
    private val lock: Any = Any()

    private val currentHashtags: List<Hashtag> get() = hashtagsMutable.value
    private val currentSuggestions: List<Suggestion> get() = suggestionsMutable.value

    init {
        val comp = CompositeDisposable()
        comp.add(
                suggestionInteractor.observeSuggestions()
                        .subscribe {
                            when (it) {
                                is SuggestionInteractor.State.Loading -> {
                                    isSuggestionsLoadingMutable.postValue(true)
                                }
                                is SuggestionInteractor.State.Loaded -> {
                                    isSuggestionsLoadingMutable.postValue(false)
                                    suggestionsMutable.postValue(it.suggestions)
                                }
                            }
                        }
        )

        suggestionInteractor.getSuggestions(emptyList(), null)
    }

    private fun getHashtagStringList(): List<String> = mutableListOf<String?>().run {
        addAll(currentHashtags.map { it.text })
        filterNotNull()
    }

    private fun getHashtagStringList(currentHashtags: List<Hashtag>): List<String> = mutableListOf<String?>().run {
        addAll(currentHashtags.map { it.text })
        filterNotNull()
    }

    override fun selectActiveHashtag(position: Int) {
        synchronized(lock) {
            val previousHashtagPosition = currentHashtagPosition
            if (previousHashtagPosition != position) {
                currentHashtagPosition = position
                val currentHashtag = currentHashtags[position]
                suggestionInteractor.getSuggestions(getHashtagStringList(), currentHashtag.text)

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
    }

    override fun selectSuggestion(position: Int) {
        synchronized(lock) {
            val selectedSuggestionText = currentSuggestions.getOrNull(position)?.value ?: return
            val newHashtags: MutableList<Hashtag> = currentHashtags
                    .mapIndexed { index, hashtag ->
                        val newText = if (index == currentHashtagPosition) selectedSuggestionText else hashtag.text
                        hashtag.copy(text = newText, state = Hashtag.State.READY)
                    }
                    .toMutableList()
                    .apply {
                        if (currentHashtagPosition == currentHashtags.lastIndex) {
                            add(Hashtag(generateId(), "#", Hashtag.State.LAST))
                        }
                    }

            suggestionInteractor.getSuggestions(getHashtagStringList(), newHashtags[currentHashtagPosition].text)
            hashtagsMutable.postValue(newHashtags)
        }
    }

    override fun deleteHashtag(position: Int) {
        synchronized(lock) {
            if (position < currentHashtagPosition) {
                currentHashtagPosition--
            }

            val newHashtags = currentHashtags
                    .toMutableList()
                    .apply { removeAt(position) }
            hashtagsMutable.postValue(newHashtags)
        }
    }

    override fun deleteFromHashtag(position: Int) {
        synchronized(lock) {
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
    }

    override fun editHashtag(hashtagPosition: Int, before: String, after: String) {
        synchronized(lock) {
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
                        suggestionInteractor.getSuggestions(getHashtagStringList(newHashtags), newHashtags.last().text)
                    } else {
                        val newHashtagState = if (hashtagPosition == currentHashtags.lastIndex) Hashtag.State.LAST else Hashtag.State.EDIT
                        val newHashtag = currentHashtags[hashtagPosition].copy(text = after, state = newHashtagState)
                        newHashtags = currentHashtags
                                .toMutableList()
                                .apply {
                                    set(hashtagPosition, newHashtag)
                                }
                        suggestionInteractor.getSuggestions(getHashtagStringList(newHashtags), newHashtag.text)
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
                                add(Hashtag(generateId(), "#", Hashtag.State.LAST, shouldGainFocus = SingleEventFlag(true)))
                            }
                    suggestionInteractor.getSuggestions(getHashtagStringList(newHashtags), null)
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
    }

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

        input.after.forEachIndexed { index, char ->
            if (!isCharValid(index, char)) {
                return HashtagInputValidation.Failure(
                        HashtagFailureReason.WRONG_SYMBOL,
                        input.before
                )
            }
        }

        if (formattedInput.length > MAX_HASHTAG_LENGTH) {
            return HashtagInputValidation.Failure(
                    HashtagFailureReason.MAX_SIZE_EXCEEDED,
                    input.after.take(MAX_HASHTAG_LENGTH)
            )
        }

        return HashtagInputValidation.Success
    }

    private val charValidation: (Char) -> Boolean = { it.isLetterOrDigit() || it == '_' }
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

    override fun keyboardAction(position: Int, action: HashtagKeyboardAction) {
        TODO()
    }

    enum class HashtagKeyboardAction {
        DELETE, ENTER
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
        private val hashtagEndChars: List<Char> = listOf(13.toChar(), ' ', '#')
    }
}

enum class HashtagFailureReason {
    MAX_SIZE_EXCEEDED,
    WRONG_SYMBOL, // "_Special symbols not allowed"
    INAPPROPRIATE_LANGUAGE // "_Inappropriate language not allowed"
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
         * Plain text hashtag which the user is entering manually.
         **/
        EDIT,

        /**
         * Hashtag which is finished.
         **/
        READY,

        /**
         * Hashtag that is selected and can be deleted by pressing back again
         **/
        SELECTED,

        /**
         * Last hashtag
         **/
        LAST
    }
}