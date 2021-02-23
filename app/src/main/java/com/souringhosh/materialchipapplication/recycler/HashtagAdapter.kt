package com.souringhosh.materialchipapplication.recycler

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.souringhosh.materialchipapplication.Hashtag
import com.souringhosh.materialchipapplication.HashtagTextWatcher
import com.souringhosh.materialchipapplication.R
import com.souringhosh.materialchipapplication.utils.extensions.exhaustive
import com.souringhosh.materialchipapplication.utils.ui.adapter.DiffCallback
import com.souringhosh.materialchipapplication.utils.ui.adapter.safeAdapterPosition

class HashtagAdapter(
        private val onHashtagDeleteClick: (Int) -> Unit,
        private val onHashtagSelected: (Int) -> Unit,
        private val hashtagTextWatcher: HashtagTextWatcher
) : RecyclerView.Adapter<HashtagAdapter.ViewHolder>() {

    var hashtags: List<Hashtag> = emptyList()
        set(value) {
            val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(DiffCallback(field, value))
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    private val textWatcher: TextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {  }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            TODO()
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            TODO("Not yet implemented")
        }
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

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.apply {
            itemView.setOnClickListener {
                safeAdapterPosition?.let {
                    val hashtag = hashtags[it]
                    if (hashtag.state != Hashtag.State.EDIT) {
                        onHashtagSelected(adapterPosition)
                    }
                }
            }
            hashtagInput.addTextChangedListener(textWatcher)
            hashtagDelete.setOnClickListener {
                safeAdapterPosition?.let { onHashtagDeleteClick(it) }
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.apply {
            itemView.setOnClickListener(null)
            hashtagDelete.setOnClickListener(null)
            hashtagInput.removeTextChangedListener(textWatcher)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val hashtagInput: EditText by lazy { view.findViewById<EditText>(R.id.hashtag_text_edit) }
        val hashtagDelete: EditText by lazy { view.findViewById<EditText>(R.id.hashtag_delete_button) }
    }

}