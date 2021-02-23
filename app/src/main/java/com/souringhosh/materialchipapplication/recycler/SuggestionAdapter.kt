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

class SuggestionAdapter(
        private val onSuggestionClick: (Int) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

    var suggestions: List<Suggestion> = emptyList()
        set(value) {
            val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(DiffCallback(field, value))
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.suggestion, parent, false)
            )

    override fun getItemCount(): Int = suggestions.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.suggestionText.text = suggestions[position].value
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onSuggestionClick(position)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.setOnClickListener(null)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val suggestionText: TextView by lazy { view.findViewById<TextView>(R.id.suggestionText) }
    }

}