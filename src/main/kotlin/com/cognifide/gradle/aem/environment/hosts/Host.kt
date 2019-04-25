package com.cognifide.gradle.aem.environment.hosts

import com.cognifide.gradle.aem.environment.EnvironmentException

class Host(ip: String, host: String) {

    init {
        if (ip.isBlank() || host.isBlank()) {
            throw EnvironmentException("Invalid hosts configuration (empty value), ip: $ip, name: $host.")
        }
    }

    val text = "$ip\t$host"
}
