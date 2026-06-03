package com.youtube.rating.shared.common

import kotlin.reflect.KClass

/**
 * Returns true if [value] is not null.
 *
 * This exists for parity with the referenced snippet, but idiomatic Kotlin is `value != null`.
 */
fun <T : Any> isNotNull(value: T?): Boolean = value != null

inline fun <T1, T2, R> multiLet(
    value1: T1?,
    value2: T2?,
    block: (T1, T2) -> R?
): R? {
    if (value1 != null && value2 != null) return block(value1, value2)
    return null
}

inline fun <T1, T2, T3, R> multiLet(
    value1: T1?,
    value2: T2?,
    value3: T3?,
    block: (T1, T2, T3) -> R?
): R? {
    if (value1 != null && value2 != null && value3 != null) return block(value1, value2, value3)
    return null
}

inline fun multiCatch(
    runThis: () -> Unit,
    catchBlock: (Throwable) -> Unit,
    vararg exceptions: KClass<out Throwable>
) {
    try {
        runThis()
    } catch (exception: Throwable) {
        val contains = exceptions.firstOrNull { it.isInstance(exception) }
        if (contains != null) catchBlock(exception) else throw exception
    }
}

