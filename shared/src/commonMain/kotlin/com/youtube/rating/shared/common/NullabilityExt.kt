package com.youtube.rating.shared.common

/**
 * Small nullability helpers.
 *
 * Note: Prefer idiomatic Kotlin (`?:`, `isNullOrBlank`, etc.) unless these improve readability.
 */
val Any?.isNull: Boolean
    get() = this == null

val Any?.isNotNull: Boolean
    get() = this != null

fun <T : Any> T?.orElse(item: T): T = this ?: item

fun <T : Any> T?.or(item: T?): T? = this ?: item

fun <T : CharSequence> T?.orElse(item: T): T =
    if (!isNullOrBlank()) this else item

fun <T : CharSequence> T?.or(item: T?): T? =
    if (!isNullOrBlank()) this else item

fun <T : Number> T?.orElse(number: T): T =
    if (this != null && this.toDouble() != 0.0) this else number

fun <T : Number> T?.or(number: T?): T? =
    if (this != null && this.toDouble() != 0.0) this else number

fun <T> List<T>?.orElse(list: List<T>): List<T> =
    if (!isNullOrEmpty()) this else list

fun <T> List<T>?.or(list: List<T>?): List<T>? =
    if (!isNullOrEmpty()) this else list

fun <K, V> Map<K, V>?.orElse(map: Map<K, V>): Map<K, V> =
    if (!isNullOrEmpty()) this else map

fun <K, V> Map<K, V>?.or(map: Map<K, V>?): Map<K, V>? =
    if (!isNullOrEmpty()) this else map

inline fun infiniteLoop(action: () -> Unit) {
    while (true) action()
}

