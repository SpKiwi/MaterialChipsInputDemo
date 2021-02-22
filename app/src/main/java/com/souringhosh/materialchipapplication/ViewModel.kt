package com.souringhosh.materialchipapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.souringhosh.materialchipapplication.repository.Suggestion
import com.souringhosh.materialchipapplication.repository.SuggestionInteractor
import com.souringhosh.materialchipapplication.utils.extensions.exhaustive
import com.souringhosh.materialchipapplication.utils.helpers.DefaultMutableLiveData
import io.reactivex.disposables.CompositeDisposable

interface ViewModel {
    val hashtagInput: LiveData<String>
    val hashtags: LiveData<List<Hashtag>>
    val errorMessage: LiveData<String?>
    val suggestions: LiveData<List<Suggestion>>
    val isSuggestionsLoading: LiveData<Boolean>
}

interface ViewModelInteractions {
    fun selectActiveHashtag(position: Int)
    fun selectHashtagInput()
    fun selectSuggestion(position: Int)

    fun deleteHashtag(position: Int)
    fun deleteFromHashtag(position: Int)
    fun deleteFromInput()
    fun editExistingHashtag(hashtagPosition: Int, text: String)
    fun editHashtagInput(text: String)
}

class ViewModelImpl(
        private val suggestionInteractor: SuggestionInteractor
) : ViewModel, ViewModelInteractions {

//    private val hashtagInputMutable: DefaultMutableLiveData<String> = DefaultMutableLiveData("#")
//    override val hashtagInput: LiveData<String> get() = hashtagInputMutable

    private val hashtagsMutable: DefaultMutableLiveData<List<Hashtag>> = DefaultMutableLiveData(emptyList())
    override val hashtags: LiveData<List<Hashtag>> get() = hashtagsMutable

    private val errorMessageMutable: MutableLiveData<String?> = MutableLiveData()
    override val errorMessage: LiveData<String?> get() = errorMessageMutable

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
        add(hashtagInput.value)
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
                            val newState = if (index == position) Hashtag.State.EDIT else Hashtag.State.READY
                            Hashtag(hashtag.text, newState)
                        }
                hashtagsMutable.postValue(newHashtags)
            }
        }
    }

    override fun selectHashtagInput() {
        synchronized(lock) {
            currentHashtagPosition = currentHashtags.size
            suggestionInteractor.getSuggestions(getHashtagStringList(), hashtagInputMutable.value)
        }
    }

    override fun selectSuggestion(position: Int) {
        synchronized(lock) {
            val selectedSuggestionText = currentSuggestions[position].value
            val newHashtags: List<Hashtag>
            if (currentHashtagPosition == currentHashtags.size) {
                newHashtags = currentHashtags
                        .map { Hashtag(it.text, Hashtag.State.READY) }
                        .toMutableList()
                        .apply {
                            add(Hashtag(selectedSuggestionText, Hashtag.State.READY))
                        }

                hashtagsMutable.postValue(newHashtags)
                hashtagInputMutable.postValue("#")
            } else {
                newHashtags = currentHashtags.mapIndexed { index, hashtag ->
                    val newText = if (currentHashtagPosition == index) selectedSuggestionText else hashtag.text
                    Hashtag(newText, Hashtag.State.READY)
                }

                hashtagsMutable.postValue(newHashtags)
            }
            suggestionInteractor.getSuggestions(getHashtagStringList(), hashtagInputMutable.value)
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

    override fun deleteFromHashtag(position: Int) { // TODO SELECTION
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

    override fun deleteFromInput() { // TODO SELECTION
        synchronized(lock) {
            if (currentHashtags.isEmpty()) {
                return
            }

            val newHashtags = currentHashtags
                    .toMutableList()
                    .apply { dropLast(1) }
            currentHashtagPosition = newHashtags.size
            hashtagsMutable.postValue(newHashtags)
        }
    }

    override fun editExistingHashtag(hashtagPosition: Int, text: String) {
//        currentHashtagPosition = position
//        val currentText = currentHashtags[currentHashtagPosition].text
//        // todo check possible deletion here удаляться таким образом может только текущий хэштен, чтобы упростить все
//        when (val validationResult = validateText(text)) {
//            is HashtagInputValidation.Success -> {
//            }
//            is HashtagInputValidation.HashtagFinished -> {
//                /** TODO
//                 *
//                 **/
//            }
//            is HashtagInputValidation.Failure -> {
//                /** TODO
//                 * 1. Update fixed hashtag text (correctedHashtag)
//                 * 2.
//                 **/
//            }
//        }.exhaustive
//        /** TODO
//         * 1. run validateText function
//         **/
    }

    override fun editHashtagInput(text: String) {
//        synchronized(lock) {
//            if (editPosition == 0) {
//                TODO("Deletion")
//            }
//            currentHashtagPosition = currentHashtags.size
//            if (hashtagInputMutable.value.length > text.length) {
//                TODO("Deletion")
//            }
//            /**
//             * check editExistingHashtag method
//             **/
//        }
    }

    private fun fixString(inputText: String): String = inputText.replace(13.toChar(), ' ') // todo check if it is needed

    private fun validateText(previousText: String?, newText: String): HashtagInputValidation {
        if (previousText == null) {
            TODO("Current text was previously empty")
        }

//        if (inputText.length == 1 && hashtagEndChars.contains(inputText[0])) { // todo check string input as well
//            TODO()
////            return HashtagInputValidation.HashtagFinished()
//        }
//
//        if (!textValidator.matches(inputText)) {
//            return HashtagInputValidation.Failure(
//                    HashtagFailureReason.WRONG_SYMBOL,
//                    currentText
//            )
//        }
//
//        if (currentText.length + inputText.length > MAX_HASHTAG_LENGTH) {
//            return HashtagInputValidation.Failure(
//                    HashtagFailureReason.MAX_SIZE_EXCEEDED,
//                    (currentText + inputText).take(MAX_HASHTAG_LENGTH)
//            )
//        }

        return HashtagInputValidation.Success
    }

    private sealed class HashtagInputValidation {
        /**
         * Text is valid, proceed with it's input
         **/
        object Success : HashtagInputValidation()

        /**
         * Space, done or hashtag symbol should convert text to READY hashtag
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
        private val textValidator: Regex = Regex("a-zA-Z0-9_")
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
) {
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