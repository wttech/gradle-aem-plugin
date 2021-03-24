package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.common.instance.service.auth.AuthException
import com.cognifide.gradle.common.CommonException
import java.util.*

class Auth(val instance: LocalInstance) {

    val credentials: Pair<String, String>
        get() = when {
            updateNeeded -> instance.user to previousPassword
            else -> instance.user to instance.password
        }

    val upToDate: Boolean get() = previousPasswordValue != null && instance.password == previousPasswordValue

    val updateNeeded: Boolean get() = previousPasswordValue != null && instance.password != previousPasswordValue

    val file get() = instance.dir.resolve("config/password.properties")

    private val previousFile get() = instance.dir.resolve("config/password-old.properties")

    private val previousPasswordValue: String?
        get() = previousFile.takeIf { it.exists() }?.let { file ->
            Properties().apply { file.inputStream().buffered().use { load(it) } }
        }?.get("admin.password")?.toString()

    val previousPassword: String
        get() = previousPasswordValue ?: throw AuthException("Previous password is not available for $instance!")

    fun update() {
        if (updateNeeded) {
            instance.sync.authManager.updatePassword(instance.user, previousPassword, instance.password)
        }
        copyPasswordFile()
    }

    private fun copyPasswordFile() = try {
        file.copyTo(previousFile, true)
    } catch (e: CommonException) {
        throw AuthException("Cannot save previous password file '$previousFile' for $instance!", e)
    }
}
