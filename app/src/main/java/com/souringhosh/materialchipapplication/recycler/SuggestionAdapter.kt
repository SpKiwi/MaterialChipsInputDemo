package com.souringhosh.materialchipapplication.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.souringhosh.materialchipapplication.R
import com.souringhosh.materialchipapplication.repository.Suggestion
import com.souringhosh.materialchipapplication.utils.ui.adapter.DiffCallback
import com.souringhosh.materialchipapplication.utils.ui.adapter.safeAdapterPosition

class SuggestionAdapter(
        private val onSuggestionClick: (Int) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.SuggestionHolder>() {

    var items: List<Suggestion> = emptyList()
        set(value) {
            val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(DiffCallback(field, value))
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionHolder =
            SuggestionHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.suggestion, parent, false)
            )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SuggestionHolder, position: Int) {
        holder.suggestionText.text = items[position].value
    }

    override fun onViewAttachedToWindow(holder: SuggestionHolder) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.setOnClickListener {
            holder.safeAdapterPosition?.let { onSuggestionClick(it) }
        }
    }

    override fun onViewDetachedFromWindow(holder: SuggestionHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.setOnClickListener(null)
    }

    class SuggestionHolder(view: View) : RecyclerView.ViewHolder(view) {
        val suggestionText: TextView by lazy { view.findViewById<TextView>(R.id.suggestion_text) }
    }

}