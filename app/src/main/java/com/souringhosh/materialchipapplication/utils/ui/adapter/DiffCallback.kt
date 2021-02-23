package com.souringhosh.materialchipapplication.utils.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

open class DiffCallback<T : ListItem>(
        private val oldList: List<T>,
        private val newList: List<T>
) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].id == newList[newItemPosition].id

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]

}

interface ListItem {
    val id: Any
}

val RecyclerView.ViewHolder.safeAdapterPosition: Int?
    get() = if (adapterPosition != RecyclerView.NO_POSITION) {
        adapterPosition
    } else {
        null
    }