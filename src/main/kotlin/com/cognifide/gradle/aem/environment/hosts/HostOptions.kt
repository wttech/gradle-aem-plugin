package com.cognifide.gradle.aem.environment.hosts

class HostOptions {

    var defined = mutableListOf<Host>()

    fun define(ip: String, name: String) {
        defined.add(Host(ip, name))
    }

    fun define(ip: String, vararg names: String) {
        define(ip, names.asIterable())
    }

    fun define(ip: String, names: Iterable<String>) {
        names.forEach { define(ip, it) }
    }

    fun define(vararg values: String) {
        define(values.asIterable())
    }

    fun define(values: Iterable<String>) {
        defined.addAll(values.map { Host.of(it) })
    }
}