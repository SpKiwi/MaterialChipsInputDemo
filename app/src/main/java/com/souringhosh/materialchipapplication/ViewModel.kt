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

/* TODO LET THE VIEWS MANAGE THEIR STATE ON THEIR OWN. KEEP CURRENT HASHTAGS LOCALLY AND DO NOT PUSH THEM INTO LIVEDATA EACH TIME */
/* TODO consider when hashtag becomes empty */
interface ViewModelInteractions {
    fun selectActiveHashtag(position: Int)
    fun selectSuggestion(position: Int)

    fun deleteHashtag(position: Int)
    fun deleteFromHashtag(position: Int)
    fun editHashtag(hashtagPosition: Int, before: String, after: String)

    /**
     * Actions like Done/Enter/Delete
     **/
    fun keyboardAction(position: Int, action: ViewModelImpl.HashtagKeyboardAction) // todo WIP
}

class ViewModelImpl(
        private val suggestionInteractor: SuggestionInteractor
) : ViewModel, ViewModelInteractions {

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

        suggestionInteractor.getSuggestions(getHashtagStringList(), "")
    }

    private fun getHashtagStringList(): List<String> = mutableListOf<String?>().run {
        addAll(currentHashtags.map { it.text })
        filterNotNull()
    }

    override fun selectActiveHashtag(position: Int) {
        synchronized(lock) {
            val previousHashtagPosition = currentHashtagPosition
            if (previousHashtagPosition != position) {
                val currentHashtag = currentHashtags[position]
                suggestionInteractor.getSuggestions(getHashtagStringList(), currentHashtag.text)

                val newHashtags = currentHashtags.toMutableList()
                        .mapIndexed { index, hashtag ->
                            val newState = when (index) {
                                position -> Hashtag.State.EDIT
                                currentHashtags.lastIndex -> Hashtag.State.LAST
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

            hashtagsMutable.postValue(newHashtags)
            suggestionInteractor.getSuggestions(getHashtagStringList(), null)
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

    override fun deleteFromHashtag(position: Int) { // TODO SELECTION (подразумевается STATE у хештега, пока без него)
        synchronized(lock) {
            if (currentHashtags.getOrNull(position - 1) == null) {
                return
            }

            currentHashtagPosition = position - 1
            val newHashtags = currentHashtags
                    .toMutableList()
                    .apply {
                        removeAt(position - 1)
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
                    val newHashtag = currentHashtags[hashtagPosition].copy(text = after, state = Hashtag.State.EDIT)
                    newHashtags = currentHashtags
                            .toMutableList()
                            .apply {
                                set(hashtagPosition, newHashtag)
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
                                add(Hashtag(generateId(), "#", Hashtag.State.LAST))
                            }
                }
                is HashtagInputValidation.Failure -> {
                    val correctedHashtag = validationResult.correctedHashtag
                    val newHashtag = currentHashtags[hashtagPosition].copy(
                            text = validationResult.correctedHashtag,
                            state = Hashtag.State.EDIT,
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
        fun isSingleHashtagSymbol(): Boolean = after.length == 1 && after[0] == ' '
    }

    private fun validateText(input: Input): HashtagInputValidation {
        if (input.isSingleHashtagSymbol()) {
            return HashtagInputValidation.Success
        }

        val formattedInput = getCleanHashtag(input.after)
        if (formattedInput.isBlank()) {
            return HashtagInputValidation.Success
        }

        if (input.isStartNewHashtag()) {
            val fixedStringBuilder = StringBuilder()
            input.after.forEachIndexed { index, char ->
                if (isCharValid(index, char))
                    fixedStringBuilder.append(char)
            }
            val fixedString = fixedStringBuilder.toString()
            if (fixedString.length > 1) {
                return HashtagInputValidation.HashtagFinished(fixedString)
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

    private fun getCleanHashtag(hashtagText: String): String {
        return if (hashtagText.startsWith('#')) {
            hashtagText.replaceFirst("#", "", true)
        } else {
            hashtagText
        }
    }

    private fun getCleanHashtagLength(hashtagText: String): Int = hashtagText.count(charValidation)

    override fun keyboardAction(position: Int, action: HashtagKeyboardAction) {
//        TODO
//        when (action) {
//            HashtagKeyboardAction.DELETE -> {
//
//            }
//            HashtagKeyboardAction.ENTER -> TODO()
//        }.exhaustive
    }

    enum class HashtagKeyboardAction { // TODO check if it is needed
        DELETE, ENTER
    }

    private sealed class HashtagInputValidation {
        /**
         * Text is valid, proceed with it's input
         **/
        object Success : HashtagInputValidation()

        /**
         * Space, hashtag symbol should convert text to READY hashtag
         **/
        data class HashtagFinished(
                val correctedHashtag: String
        ) : HashtagInputValidation()

        /**
         * Something wrong with text, should delete/correct it
         **/
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
        val shouldCorrectSpelling: SingleEventFlag = SingleEventFlag()
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