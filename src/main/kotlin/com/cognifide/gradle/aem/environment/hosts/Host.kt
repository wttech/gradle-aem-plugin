package com.cognifide.gradle.aem.environment.hosts

import com.cognifide.gradle.aem.environment.EnvironmentException
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable

class Host(val ip: String, val name: String) : Serializable {

    init {
        if (ip.isBlank() || name.isBlank()) {
            throw EnvironmentException("Invalid hosts configuration (empty value), ip: $ip, name: $name.")
        }
    }

    @get:JsonIgnore
    val text: String
        get() = "$ip\t$name"

    companion object {
        fun of(value: String): Host {
            val parts = value.trim().split(" ").map { it.trim() }
            if (parts.size != 2) {
                throw EnvironmentException("Invalid hosts value: '$value'")
            }
            val (ip, name) = parts

            return Host(ip, name)
        }
    }
}
