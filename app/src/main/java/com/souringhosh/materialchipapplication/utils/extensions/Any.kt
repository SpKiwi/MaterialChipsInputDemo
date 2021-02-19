package com.souringhosh.materialchipapplication.utils.extensions

/**
 * Used to guarantee a specific result in expressions
 **/
val <T> T.exhaustive: T
    get() = this