package com.cognifide.gradle.aem.environment.hosts

class HostsOptions {

    var defined = mutableListOf<Host>()

    fun define(values: Iterable<String>) {
        defined.addAll(values.map { Host.of(it) })
    }
}