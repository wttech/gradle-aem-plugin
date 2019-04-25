package com.cognifide.gradle.aem.environment.hosts

class HostsOptions {
    var list = listOf(
            Host("127.0.0.1", "example.com"),
            Host("127.0.0.1", "demo.example.com"),
            Host("127.0.0.1", "author.example.com"),
            Host("127.0.0.1", "invalidation-only")
    )

    fun configure(config: Map<String, String>) {
        list = config.map { Host(it.key, it.value) }
    }
}