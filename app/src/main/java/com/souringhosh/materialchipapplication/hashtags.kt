package com.souringhosh.materialchipapplication

import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.widget.EditText

class HashtagEditText {

    var maxSuggestionCount: Int = 0
        set(value) {
            if (field == value) {
                return
            }

            field = if (value < 0) {
                0
            } else {
                value
            }
            refreshHashtags()
        }

    private fun refreshHashtags() {

    }

    fun setTextValidator(textValidator: Nothing) {
        TODO()
    }

    fun onTextEnter(enteredText: String) {
        when (val validationResult = isTextValid(enteredText)) {
            is TextValidationResult.Valid -> {
                clearAnyErrors()
                performFiltering()
            }
            is TextValidationResult.HashtagFinished -> {
                clearAnyErrors()
                createHashtag()
                startNewHashtag()
                showPopularResults()
            }
            is TextValidationResult.Failed -> {
                showIncorrectTextError(validationResult.reason)
                deleteIncorrectText(enteredText)
            }
        }
    }

    private fun clearAnyErrors() {
        TODO()
    }

    private fun performFiltering() {
        TODO()
    }

    private fun createHashtag() {
        TODO()
    }

    private fun startNewHashtag() {
        TODO()
    }

    private fun showPopularResults() {
        TODO()
    }

    private fun showIncorrectTextError(reason: String) {
        TODO()
    }

    private fun deleteIncorrectText(incorrectText: String) {
        TODO()
    }

    private fun isTextValid(enteredText: String): TextValidationResult {
        TODO()
    }

    private sealed class TextValidationResult { // todo fix namings
        /**
         * Text is valid, proceed with it's input
         **/
        object Valid : TextValidationResult()
        /**
         * Space, done or hashtag symbol press action, should convert text to READY hashtag
         **/
        object HashtagFinished : TextValidationResult()
        /**
         * Something wrong with text, should delete it
         **/
        data class Failed(val reason: String) : TextValidationResult()
    }

    private val hashtags: MutableList<Hashtag> = mutableListOf()
    private var currentHashtag: Hashtag? = null

    private data class Hashtag(
            val text: String,
            val state: State,
            val startPosition: Int,
            val endPosition: Int
    ) {
        val length: Int get() = (endPosition + 1) - startPosition

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
            SELECTED
        }
    }

    interface HashtagEditCallbacks {
//        fun onHashtagTextEnter(text: String, )
        /**
         * Called when hashtag is selected for further edit.
         **/
        fun onHashtagSelect(position: Int)
        /**
         * Called when hashtag is deleted
         **/
        fun onHashtagDelete(position: Int)
        /**
         * Called when the suggestion for hashtag is selected
         **/
        fun onSuggestionSelect(position: Int)
    }

    fun some() {
        val tw = object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                TODO("Not yet implemented")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                TODO("Not yet implemented")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                TODO("Not yet implemented")
            }
        }
    }

    @Suppress("UNREACHABLE_CODE")
    fun some1() {
        val editText: EditText = TODO()
        val span: ImageSpan = TODO()
        val text = editText.text!!
        text.setSpan(span, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

}