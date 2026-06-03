package com.youtube.rating.android.sentry

import io.sentry.Sentry
import io.sentry.protocol.User
import java.security.MessageDigest

object SentryUserContextProvider {
    fun setUserToken(token: String?) {
        setUserContext(token = token, installId = null)
    }

    fun setUserContext(token: String?, installId: String?) {
        val tokenId = token?.takeIf { it.isNotBlank() }?.let { hashString(value = it) }
        val installIdHash = installId?.takeIf { it.isNotBlank() }?.let { hashString(value = it) }

        val user = when {
            tokenId != null -> User().apply { id = tokenId }
            installIdHash != null -> User().apply { id = installIdHash }
            else -> null
        }

        Sentry.setUser(user)
    }

    fun hashForTag(value: String): String = hashString(value = value)

    private fun hashString(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
