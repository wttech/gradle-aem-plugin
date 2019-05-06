package com.cognifide.gradle.aem.environment.health

import org.apache.commons.lang3.StringUtils

class HealthStatus(val check: HealthCheck, val cause: Exception? = null) {

    val passed: Boolean
        get() = cause == null

    val status: String
        get() = if (cause == null) {
            "OK"
        } else {
            "FAIL | ${StringUtils.abbreviate(cause.message, 120)}"
        }

    override fun toString(): String {
        return "$check | $status".trim()
    }
}