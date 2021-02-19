package com.souringhosh.materialchipapplication.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.souringhosh.materialchipapplication.R

class SuggestionAdapter(
        private val onSuggestionClick: (Int) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

    var suggestions: List<String> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.suggestion, parent, false)
            )

    override fun getItemCount(): Int = suggestions.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.suggestionText.text = suggestions[position]
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val suggestionText: TextView by lazy { view.findViewById<TextView>(R.id.suggestionText) }
    }

}