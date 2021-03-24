package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.common.instance.service.auth.AuthException
import com.cognifide.gradle.common.CommonException
import java.util.*

class Auth(val instance: LocalInstance) {

    val credentials: Pair<String, String>
        get() = when {
            !available -> Instance.CREDENTIALS_DEFAULT
            !upToDate -> instance.user to previousPassword
            else -> instance.user to instance.password
        }

    val available: Boolean get() = instance.initialized || instance.locked(LOCK_NAME)

    fun becameAvailable(): Boolean {
        if (!available && instance.credentials != Instance.CREDENTIALS_DEFAULT) {
            if (instance.sync.status.checkUnauthorized()) {
                instance.lock(LOCK_NAME)
                return true
            }
        }
        return false
    }

    val upToDate: Boolean get() = previousPasswordValue != null && instance.password == previousPasswordValue

    val updateNeeded: Boolean get() = previousPasswordValue != null && instance.password != previousPasswordValue

    val file get() = instance.dir.resolve("config/password.properties")

    private val previousFile get() = file.parentFile.resolve("${file.name}.previous")

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

    companion object {
        const val LOCK_NAME = "auth"
    }
}
