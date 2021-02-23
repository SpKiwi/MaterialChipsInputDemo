package com.souringhosh.materialchipapplication.utils.helpers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class DefaultLiveData<T>(
        defaultValue: T
) : LiveData<T>() {
    init {
        value = defaultValue
    }

    override fun getValue(): T {
        return super.getValue()!!
    }
}

class DefaultMutableLiveData<T>(
        defaultValue: T
) : MutableLiveData<T>() {
    init {
        value = defaultValue
    }

    override fun getValue(): T {
        return super.getValue()!!
    }
}