package com.souringhosh.materialchipapplication.recycler

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.souringhosh.materialchipapplication.Hashtag
import com.souringhosh.materialchipapplication.KeyCallbacks
import com.souringhosh.materialchipapplication.OnHashtagEditListener
import com.souringhosh.materialchipapplication.R
import com.souringhosh.materialchipapplication.utils.extensions.exhaustive
import com.souringhosh.materialchipapplication.utils.ui.adapter.DiffCallback
import com.souringhosh.materialchipapplication.utils.ui.adapter.nullableAdapterPosition

class HashtagAdapter(
        private val onHashtagDeleteClick: (Int) -> Unit,
        private val onHashtagSelected: (Int) -> Unit,
        private val onHashtagEditListener: OnHashtagEditListener,
        private val keyCallbacks: KeyCallbacks
) : RecyclerView.Adapter<HashtagAdapter.HashtagHolder>() {

    var items: List<Hashtag> = emptyList()
        set(value) {
            val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(HashtagDiffCallback(field, value))
            field = value

            preventTextNotify {
                diffResult.dispatchUpdatesTo(this)
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HashtagHolder =
            HashtagHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.hashtag_item, parent, false)
            )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: HashtagHolder, position: Int) {
        val hashtag = items[position]
        preventTextNotify {
            holder.bind(hashtag)
        }
    }

    override fun onBindViewHolder(holder: HashtagHolder, position: Int, payloads: MutableList<Any>) {
        val hashtag = items[position]
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val bundle = payloads[0] as Bundle
            bundle.keySet().forEach { key ->
                when (key) {
                    HASHTAG_NEW_STATE -> holder.bindState(hashtag.state)
                    HASHTAG_NEW_TEXT -> {
                        preventTextNotify {
                            holder.bindText(hashtag.text)
                        }
                    }
                }
            }
        }
        if (hashtag.shouldGainFocus.value) {
            holder.hashtagInput.requestFocus()
        }
    }

    private var isTextWatcherEnabled: Boolean = true

    private fun preventTextNotify(block: () -> Unit) {
        isTextWatcherEnabled = false
        block()
        isTextWatcherEnabled = true
    }

    /* Map of Position to Hashtag text before change was made */
    private val beforeTexts: MutableMap<Int, String> = mutableMapOf()

    override fun onViewAttachedToWindow(holder: HashtagHolder) {
        super.onViewAttachedToWindow(holder)
        holder.apply {
            /* Selection */
            hashtagInput.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    nullableAdapterPosition?.let {
                        onHashtagSelected(it)
                    }
                }
            }
            /* Delete */
            hashtagDelete.setOnClickListener {
                nullableAdapterPosition?.let { onHashtagDeleteClick(it) }
            }
            /* Text input */
            val textWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    if (isTextWatcherEnabled) {
                        nullableAdapterPosition?.let {
                            val beforeText = s?.toString() ?: ""
                            beforeTexts[it] = beforeText
                        }
                    }
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (isTextWatcherEnabled) {
                        nullableAdapterPosition?.let {
                            val beforeText = beforeTexts.remove(it) ?: ""
                            val afterText = s?.toString() ?: ""
                            onHashtagEditListener.onHashtagEdit(
                                    position = it,
                                    before = beforeText,
                                    after = afterText
                            )
                        }
                    }
                }
            }
            hashtagInput.addTextChangedListener(textWatcher)
            hashtagInput.tag = textWatcher
            /* Keyboard */
            hashtagInput.setOnKeyListener { _, _, event ->
                if (event != null && event.action == KeyEvent.ACTION_DOWN) {
                    if (event.keyCode == KeyEvent.KEYCODE_DEL && hashtagInput.selectionStart == 0) {
                        nullableAdapterPosition?.let { keyCallbacks.onDeletePressed(it) }
                    } else if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                        nullableAdapterPosition?.let { keyCallbacks.onFinishInputPresses(it) }
                    }
                }
                false
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: HashtagHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.apply {
            itemView.onFocusChangeListener = null
            hashtagDelete.setOnClickListener(null)
            val textWatcher = hashtagInput.tag as? TextWatcher
            hashtagInput.removeTextChangedListener(textWatcher)
            hashtagInput.setOnKeyListener(null)
        }
    }

    class HashtagHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hashtagInput: EditText = itemView.findViewById<EditText>(R.id.hashtag_text_edit)
        val hashtagDelete: ImageView = itemView.findViewById<ImageView>(R.id.hashtag_delete_button)

        fun bind(hashtag: Hashtag) {
            bindText(hashtag.text)
            bindState(hashtag.state)
        }

        fun bindText(text: String) {
            val previousCursorPosition: Int? = if (hashtagInput.isFocused) {
                hashtagInput.selectionStart
            } else {
                null
            }

            if (hashtagInput.text.toString() != text) {
                hashtagInput.setText(text)
                previousCursorPosition?.let {
                    if (it > text.length) {
                        hashtagInput.setSelection(text.length)
                    } else if (it - POSITION_OFFSET >= 0) {
                        hashtagInput.setSelection(it - POSITION_OFFSET)
                    }
                }
            }
        }

        fun bindState(state: Hashtag.State) {
            when (state) {
                Hashtag.State.EDIT -> {
                    hashtagInput.setTextColor(Color.YELLOW)
                    hashtagDelete.visibility = View.GONE
                }
                Hashtag.State.LAST -> {
                    hashtagInput.setTextColor(Color.GREEN)
                    hashtagDelete.visibility = View.GONE
                }
                Hashtag.State.READY -> {
                    hashtagInput.setTextColor(Color.RED)
                    hashtagDelete.visibility = View.VISIBLE
                }
                Hashtag.State.SELECTED -> {
                    TODO()
                }
            }.exhaustive
        }

        companion object {
            private const val POSITION_OFFSET = 1 // todo consider case of larger offset (for example copy/paste)
        }
    }

    private class HashtagDiffCallback(
            private val oldList: List<Hashtag>,
            private val newList: List<Hashtag>
    ) : DiffCallback<Hashtag>(oldList, newList) {

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldElement = oldList[oldItemPosition]
            val newElement = newList[newItemPosition]

            val diff = Bundle().apply {
                if (oldElement.text != newElement.text || newElement.shouldCorrectSpelling.value) {
                    putByte(HASHTAG_NEW_TEXT, -1)
                }
                if (oldElement.state != newElement.state) {
                    putByte(HASHTAG_NEW_STATE, -1)
                }
            }
            return if (diff.size() == 0) null else diff
        }
    }

    companion object {
        const val HASHTAG_NEW_STATE = "HASHTAG_NEW_STATE"
        const val HASHTAG_NEW_TEXT = "HASHTAG_NEW_TEXT"
    }

}