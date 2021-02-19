package com.souringhosh.materialchipapplication

import android.content.Context
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView

class HashtagView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val hashtagTextEdit: EditText by lazy { findViewById<EditText>(R.id.hashtag_text_edit) }
    private val hashtagDeleteButton: ImageView by lazy {
        findViewById<ImageView>(R.id.hashtag_delete_button).apply { enlargeHitArea() }
    }

    init {
        View.inflate(context, R.layout.hashtag, this)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        hashtagTextEdit.isEnabled = enabled
        hashtagDeleteButton.isEnabled = enabled
    }

    fun addTextChangedListener(textWatcher: TextWatcher) {
        hashtagTextEdit.addTextChangedListener(textWatcher)
    }

    fun setText(text: String) {
        hashtagTextEdit.setText(text)
    }

    fun setOnCloseIconClickListener(onClickListener: OnClickListener) {
        hashtagDeleteButton.setOnClickListener(onClickListener)
    }

    fun setOnCloseIconClickListener(onClickListener: (View) -> Unit) {
        hashtagDeleteButton.setOnClickListener(onClickListener)
    }

}