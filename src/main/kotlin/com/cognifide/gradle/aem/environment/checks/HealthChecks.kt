package com.cognifide.gradle.aem.environment.checks

class HealthChecks {
    val list = mutableListOf<HealthCheck>()

    infix fun String.respondsWith(block: HealthCheck.() -> Unit) {
        list += HealthCheck(this).apply(block)
    }
}