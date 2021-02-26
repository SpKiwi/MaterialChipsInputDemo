package com.souringhosh.materialchipapplication.utils.helpers

import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<T>.startWith(value: T): MutableLiveData<T> = this.apply {
    postValue(value)
}