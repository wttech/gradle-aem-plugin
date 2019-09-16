package com.cognifide.gradle.aem.environment.health

class HealthCheck(val name: String, val action: () -> Any?) {

    @Suppress("TooGenericExceptionCaught")
    fun perform(): HealthStatus {
        var cause: Exception? = null
        try {
            action()
        } catch (e: Exception) {
            cause = e
        }

        return HealthStatus(this, cause)
    }

    override fun toString(): String = name
}
