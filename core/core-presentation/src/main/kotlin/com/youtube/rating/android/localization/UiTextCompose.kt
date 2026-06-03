package com.youtube.rating.android.localization

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

sealed class UiText {
    data class Dynamic(val value: String) : UiText()
    data class StringResource(@StringRes val resId: Int, val args: List<Any> = emptyList()) : UiText()

    fun resolve(context: Context): String {
        return when (this) {
            is Dynamic -> value
            is StringResource -> context.getString(resId, *args.toTypedArray())
        }
    }

    companion object {
        fun from(plain: String?): UiText? = plain?.let { Dynamic(value = it) }
    }
}

@Composable
fun UiText.asString(): String {
    val context = LocalContext.current
    return resolve(context = context)
}
