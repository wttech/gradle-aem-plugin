package com.cognifide.gradle.aem.environment.health

class HealthStatus(val check: HealthCheck, val cause: Exception? = null) {

    val passed: Boolean
        get() = cause == null

    val status: String
        get() = if (cause == null) {
            "OK"
        } else {
            "FAIL | ${cause.message}"
        }

    override fun toString(): String {
        return "$check | $status".trim()
    }
}
