package com.souringhosh.materialchipapplication.recycler

import android.graphics.Color
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
            val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(DiffCallback(field, value))
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
        holder.apply {
            hashtagInput.setText(hashtag.text)
            when (hashtag.state) {
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
                    safeAdapterPosition?.let {
                        val beforeText = s?.toString() ?: ""
                        beforeTexts[it] = beforeText
                    }
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    safeAdapterPosition?.let {
                        val beforeText = beforeTexts[it] ?: ""
                        val afterText = s?.toString() ?: ""
                        editHashtag(it, beforeText, afterText)
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
    }

}