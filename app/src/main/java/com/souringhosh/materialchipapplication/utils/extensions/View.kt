package com.souringhosh.materialchipapplication.utils.extensions

import android.graphics.Rect
import android.view.TouchDelegate
import android.view.View
import com.souringhosh.materialchipapplication.R

fun View.enlargeHitArea() {
    val pixels = resources.getDimensionPixelSize(R.dimen.default_enlarge_size)

    (parent as? View)?.post {
        val rect = Rect()
        getHitRect(rect)
        rect.apply {
            top -= pixels
            left -= pixels
            bottom += pixels
            right += pixels
        }
        (parent as View).touchDelegate = TouchDelegate(rect, this)
    }
}