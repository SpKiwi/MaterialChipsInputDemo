package com.souringhosh.materialchipapplication.recycler

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.souringhosh.materialchipapplication.Hashtag
import com.souringhosh.materialchipapplication.KeyCallbacks
import com.souringhosh.materialchipapplication.R
import com.souringhosh.materialchipapplication.utils.extensions.exhaustive
import com.souringhosh.materialchipapplication.utils.ui.adapter.DiffCallback
import com.souringhosh.materialchipapplication.utils.ui.adapter.safeAdapterPosition
import com.souringhosh.materialchipapplication.views.SimpleEditText

class HashtagAdapter(
        private val onHashtagDeleteClick: (Int) -> Unit,
        private val onHashtagSelected: (Int) -> Unit,
        private val editHashtag: (Int, String, String) -> Unit,
        private val keyCallbacks: KeyCallbacks
) : RecyclerView.Adapter<HashtagAdapter.ViewHolder>() {

    var hashtags: List<Hashtag> = emptyList()
        set(value) {
            val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(HashtagDiffCallback(field, value))
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.hashtag, parent, false)
            )

    override fun getItemCount(): Int = hashtags.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hashtag = hashtags[position]
        holder.bind(hashtag)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val bundle = payloads[0] as Bundle
            bundle.keySet().forEach { key ->
                when (key) {
                    HASHTAG_NEW_STATE -> holder.bindState(hashtags[position].state)
                    HASHTAG_NEW_TEXT -> {
                        preventTextNotify {
                            holder.bindText(hashtags[position].text)
                        }
                    }
                }
            }
        }
    }

    private var isTextWatcherEnabled: Boolean = true

    @Synchronized
    private fun preventTextNotify(block: () -> Unit) {
        isTextWatcherEnabled = false
        block()
        isTextWatcherEnabled = true
    }

    /* Position */
    private val beforeTexts: MutableMap<Int, String> = mutableMapOf()

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.apply {
            /* Selection */
            itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    safeAdapterPosition?.let {
                        onHashtagSelected(it)
                    }
                }
            }
            /* Delete */
            hashtagDelete.setOnClickListener {
                safeAdapterPosition?.let { onHashtagDeleteClick(it) }
            }
            /* Text input */
            hashtagInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    if (isTextWatcherEnabled) {
                        safeAdapterPosition?.let {
                            val beforeText = s?.toString() ?: ""
                            beforeTexts[it] = beforeText
                        }
                    }
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (isTextWatcherEnabled) {
                        safeAdapterPosition?.let {
                            val beforeText = beforeTexts.remove(it) ?: ""
                            val afterText = s?.toString() ?: ""
                            editHashtag(it, beforeText, afterText)
                        }
                    }
                }
            })
            /* Keyboard */
            hashtagInput.setOnKeyListener { _, _, event ->
                if (event != null && event.action == KeyEvent.ACTION_DOWN) {
                    if (event.keyCode == KeyEvent.KEYCODE_DEL && hashtagInput.selectionStart == 0) {
                        safeAdapterPosition?.let { keyCallbacks.onDeletePressed(it) }
                    } else if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                        safeAdapterPosition?.let { keyCallbacks.onFinishInputPresses(it) }
                    }
                }
                false
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.apply {
            itemView.onFocusChangeListener = null
            hashtagDelete.setOnClickListener(null)
            hashtagInput.clearTextChangedListeners()
            hashtagInput.setOnKeyListener(null)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val hashtagInput: SimpleEditText by lazy { view.findViewById<SimpleEditText>(R.id.hashtag_text_edit) }
        val hashtagDelete: ImageView by lazy { view.findViewById<ImageView>(R.id.hashtag_delete_button) }

        fun bind(hashtag: Hashtag) {
            bindText(hashtag.text)
            bindState(hashtag.state)
        }

        fun bindText(text: String) {
            if (hashtagInput.text.toString() != text) {
                hashtagInput.setText(text)
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
    }

    private class HashtagDiffCallback(
            private val oldList: List<Hashtag>,
            private val newList: List<Hashtag>
    ) : DiffCallback<Hashtag>(oldList, newList) {

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldElement = oldList[oldItemPosition]
            val newElement = newList[newItemPosition]

            val diff = Bundle().apply {
                if (oldElement.text != newElement.text) {
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