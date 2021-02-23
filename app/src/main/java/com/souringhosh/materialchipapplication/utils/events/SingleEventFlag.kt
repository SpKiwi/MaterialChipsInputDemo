package com.souringhosh.materialchipapplication.utils.events

data class SingleEventFlag(
        private var initialValue: Boolean = false
) {

    var value: Boolean = initialValue
        private set
        @Synchronized
        get() {
            if (!initialValue)
                return false

            if (initialValue && field) {
                value = false
                initialValue = false
                return true
            } else {
                return false
            }
        }

}