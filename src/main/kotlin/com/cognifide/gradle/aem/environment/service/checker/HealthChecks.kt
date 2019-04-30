package com.cognifide.gradle.aem.environment.service.checker

class HealthChecks {

    val list = mutableListOf<HealthCheck>()

    infix fun String.respondsWith(block: HealthCheck.() -> Unit) {
        list += HealthCheck(this).apply(block)
    }
}