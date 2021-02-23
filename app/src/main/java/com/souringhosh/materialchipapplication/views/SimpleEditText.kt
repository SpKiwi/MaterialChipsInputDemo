package com.souringhosh.materialchipapplication.views

import android.content.Context
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import java.util.*

class SimpleEditText @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatEditText(context, attrs, defStyleAttr) {
    private var listeners: ArrayList<TextWatcher>? = null

    override fun addTextChangedListener(watcher: TextWatcher) {
        if (listeners == null) {
            listeners = ArrayList()
        }
        listeners!!.add(watcher)
        super.addTextChangedListener(watcher)
    }

    override fun removeTextChangedListener(watcher: TextWatcher) {
        if (listeners != null) {
            val i = listeners!!.indexOf(watcher)
            if (i >= 0) {
                listeners!!.removeAt(i)
            }
        }
        super.removeTextChangedListener(watcher)
    }

    fun clearTextChangedListeners() {
        if (listeners != null) {
            for (watcher in listeners!!) {
                super.removeTextChangedListener(watcher)
            }
            listeners!!.clear()
            listeners = null
        }
    }

}