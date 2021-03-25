package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.common.instance.service.auth.AuthException
import com.cognifide.gradle.common.CommonException
import java.util.*

class Auth(val instance: LocalInstance) {

    val credentials: Pair<String, String>
        get() = when {
            updateNeeded -> instance.user to passwordPrevious
            else -> instance.user to instance.password
        }

    val updateNeeded: Boolean get() = instance.password != passwordPrevious

    val passwordPrevious: String get() = passwordProperty ?: Instance.PASSWORD_DEFAULT

    fun update() {
        if (updateNeeded) {
            instance.sync.authManager.updatePassword(instance.user, passwordPrevious, instance.password)
            saveProperties()
        }
    }

    private val passwordProperty: String? get() = properties["admin.password"]?.toString()

    private val properties get() = Properties().apply {
        if (propertiesFile.exists()) {
            propertiesFile.inputStream().buffered().use { load(it) }
        }
    }

    private val propertiesFile get() = instance.dir.resolve("config/auth.properties")

    private fun saveProperties() = try {
        propertiesFile.parentFile.mkdirs()
        propertiesFile.bufferedWriter().use {
            Properties().apply { put("admin.password", instance.password) }.store(it, null)
        }
    } catch (e: CommonException) {
        throw AuthException("Cannot save auth properties to file '$propertiesFile' for $instance!", e)
    }
}
