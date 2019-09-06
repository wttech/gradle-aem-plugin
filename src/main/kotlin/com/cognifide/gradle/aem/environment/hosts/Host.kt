package com.cognifide.gradle.aem.environment.hosts

import com.cognifide.gradle.aem.environment.EnvironmentException
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import java.net.URL

class Host(val url: String) : Serializable {

    var ip = "127.0.0.1"

    var tags = mutableSetOf<String>()

    @get:JsonIgnore
    val config = URL(url)

    init {
        if (url.isBlank()) {
            throw EnvironmentException("Host URL cannot be blank!")
        }
    }

    @get:JsonIgnore
    val text: String
        get() = "$ip\t${config.host}"

    fun tag(id: String) {
        tags.add(id)
    }

    fun tag(ids: Iterable<String>) {
        tags.addAll(ids)
    }

    fun tag(vararg ids: String) = tag(ids.asIterable())
}
