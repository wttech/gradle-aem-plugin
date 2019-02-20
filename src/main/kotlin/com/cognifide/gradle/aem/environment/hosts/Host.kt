package com.cognifide.gradle.aem.environment.hosts

import com.cognifide.gradle.aem.environment.EnvironmentException

class Host(ipAddress: String, hostName: String) {

    init {
        if (ipAddress.isBlank() || hostName.isBlank()) {
            throw EnvironmentException("Invalid hosts configuration (empty value), ip: $ipAddress, name: $hostName.")
        }
    }

    val text = "$ipAddress\t$hostName"
}
