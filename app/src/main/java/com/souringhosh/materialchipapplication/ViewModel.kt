package com.souringhosh.materialchipapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.souringhosh.materialchipapplication.repository.Suggestion
import com.souringhosh.materialchipapplication.repository.SuggestionInteractor
import com.souringhosh.materialchipapplication.utils.extensions.exhaustive
import com.souringhosh.materialchipapplication.utils.helpers.DefaultMutableLiveData
import com.souringhosh.materialchipapplication.utils.ui.adapter.ListItem
import io.reactivex.disposables.CompositeDisposable

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
    fun deleteFromHashtag(position: Int) // todo call from activity
    fun editHashtag(hashtagPosition: Int, before: String, after: String)
    /**
     * Actions like Done/Enter/Delete
     **/
    fun keyboardAction(position: Int, action: ViewModelImpl.HashtagKeyboardAction) // todo WIP
}

class ViewModelImpl(
        private val suggestionInteractor: SuggestionInteractor
) : ViewModel, ViewModelInteractions {

    private val hashtagsMutable: DefaultMutableLiveData<List<Hashtag>> = DefaultMutableLiveData(emptyList())
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
                            Hashtag(hashtag.text, newState)
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
                        Hashtag(newText, Hashtag.State.READY)
                    }
                    .toMutableList()
                    .apply {
                        if (currentHashtagPosition == currentHashtags.lastIndex) {
                            add(Hashtag("#", Hashtag.State.LAST))
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
                    val newHashtag = Hashtag(after, Hashtag.State.EDIT)
                    newHashtags = currentHashtags
                            .toMutableList()
                            .apply {
                                set(hashtagPosition, newHashtag)
                            }
                }
                is HashtagInputValidation.HashtagFinished -> {
                    val newHashtag = Hashtag(validationResult.correctedHashtag, Hashtag.State.READY)
                    newHashtags = currentHashtags
                            .toMutableList()
                            .apply {
                                set(hashtagPosition, newHashtag)
                                add(Hashtag("#", Hashtag.State.LAST))
                            }
                }
                is HashtagInputValidation.Failure -> {
                    val newHashtag = Hashtag(validationResult.correctedHashtag, Hashtag.State.EDIT)
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
        val length: Int get() = after.length
        fun isStartNewHashtag(): Boolean = after.length - before.length == 1 && hashtagEndChars.contains(after.last())
    }

    private fun validateText(input: Input): HashtagInputValidation {
        if (input.isStartNewHashtag()) {
            val fixedString = "#${allowedCharacters.replace(input.after, "")}"
            return HashtagInputValidation.HashtagFinished(fixedString)
        }

        if (!allowedCharacters.matches(input.after)) {
            return HashtagInputValidation.Failure(
                    HashtagFailureReason.WRONG_SYMBOL,
                    input.before
            )
        }

        if (input.length > MAX_HASHTAG_LENGTH) {
            return HashtagInputValidation.Failure(
                    HashtagFailureReason.MAX_SIZE_EXCEEDED,
                    input.after.take(MAX_HASHTAG_LENGTH)
            )
        }

        return HashtagInputValidation.Success
    }

    override fun keyboardAction(position: Int, action: HashtagKeyboardAction) {
//        TODO
//        when (action) {
//            HashtagKeyboardAction.DELETE -> {
//
//            }
//            HashtagKeyboardAction.ENTER -> TODO()
//        }.exhaustive
    }

    enum class HashtagKeyboardAction { // TODO check if it is neeeded
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
        private const val MAX_HASHTAG_LENGTH = 50
        private val allowedCharacters: Regex = Regex("a-zA-Z0-9_") // ^[a-zA-Z\\s]*$ // todo check regex
        private val hashtagEndChars: List<Char> = listOf(13.toChar(), ' ', '#')
    }
}

enum class HashtagFailureReason {
    MAX_SIZE_EXCEEDED,
    WRONG_SYMBOL, // "_Special symbols not allowed"
    INAPPROPRIATE_LANGUAGE // "_Inappropriate language not allowed"
}

data class Hashtag(
        val text: String,
        val state: State
) : ListItem {
    override val id: Any get() = text

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