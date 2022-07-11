package com.kieronquinn.app.discoverkiller.utils.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

suspend fun <T> Flow<T?>.firstNotNull(): T {
    return first { it != null }!!
}