package com.souringhosh.materialchipapplication.utils.helpers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class DefaultLiveData<T>(
        defaultValue: T
) : LiveData<T>() {
    init {
        postValue(defaultValue)
    }

    override fun getValue(): T {
        return super.getValue()!!
    }
}

class DefaultMutableLiveData<T>(
        defaultValue: T
) : MutableLiveData<T>() {
    init {
        postValue(defaultValue)
    }

    override fun getValue(): T {
        return super.getValue()!!
    }
}