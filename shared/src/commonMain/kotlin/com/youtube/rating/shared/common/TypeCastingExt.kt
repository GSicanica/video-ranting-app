package com.youtube.rating.shared.common

fun Any.toUnsafeInt(): Int =
    when (val value = this) {
        is String -> value.toInt()
        is Int -> value
        is Float -> value.toInt()
        is Double -> value.toInt()
        is Long -> value.toInt()
        else -> throw NumberFormatException("Cannot convert ${value::class} to Int")
    }

@Suppress("UNCHECKED_CAST")
fun <T> Any?.asType(): T? = this as? T

fun Any.toNumberOrNull(): Number? =
    when (this) {
        is Number -> this
        is String -> this.toIntOrNull()
            ?: this.toLongOrNull()
            ?: this.toDoubleOrNull()
            ?: this.toFloatOrNull()
        else -> null
    }

fun Any.toBooleanOrNull(): Boolean? =
    when (this) {
        is String -> when {
            equals("true", true) -> true
            equals("false", true) -> false
            equals("1", true) -> true
            equals("0", true) -> false
            else -> null
        }
        is Boolean -> this
        is Number -> when (toInt()) {
            1 -> true
            0 -> false
            else -> null
        }
        else -> null
    }

inline fun <T> tryOrNull(block: () -> T): T? =
    try {
        block()
    } catch (_: Throwable) {
        null
    }

inline fun <reified T : Enum<T>> enumSafeValueOf(name: String): T? =
    tryOrNull { enumValueOf<T>(name) }
