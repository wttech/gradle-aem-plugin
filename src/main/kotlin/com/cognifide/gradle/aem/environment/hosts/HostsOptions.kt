package com.cognifide.gradle.aem.environment.hosts

import com.cognifide.gradle.aem.environment.EnvironmentException
import org.apache.commons.lang3.SystemUtils

class HostsOptions {
    var list = listOf(
        Host("127.0.0.1", "example.com"),
        Host("127.0.0.1", "demo.example.com"),
        Host("127.0.0.1", "author.example.com"),
        Host("127.0.0.1", "invalidation-only")
    )

    val file = if (SystemUtils.IS_OS_WINDOWS) {
        HOSTS_FILE_ON_WINDOWS
    } else {
        HOSTS_FILE
    }

    fun configure(config: Map<String, String>) {
        config.forEach { hostEntry ->
            if (hostEntry.key.isBlank() || hostEntry.value.isBlank()) {
                throw EnvironmentException("Invalid hosts configuration (empty value), ip: ${hostEntry.key}, name: ${hostEntry.value}. ")
            }
        }
        list = config.map { Host(it.key, it.value) }
    }

    companion object {
        const val HOSTS_FILE_ON_WINDOWS = "C:\\Windows\\System32\\drivers\\etc\\hosts"
        const val HOSTS_FILE = "/etc/hosts"
    }
}