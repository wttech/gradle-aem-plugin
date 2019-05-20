package com.cognifide.gradle.aem.environment.docker.base

import com.cognifide.gradle.aem.common.AemExtension

enum class DockerType(val hostIp: String) {
    DESKTOP("127.0.0.1"),
    TOOLBOX("192.168.99.100");

    override fun toString(): String = name.toLowerCase()

    companion object {

        fun determine(aem: AemExtension): DockerType {
            return aem.props.string("aem.environment.docker.type")?.let { of(it) } ?: detect() ?: DESKTOP
        }

        fun of(name: String): DockerType? {
            return values().firstOrNull { it.name.equals(name, true) }
        }

        fun detect(): DockerType? {
            if (!System.getenv("DOCKER_TOOLBOX_INSTALL_PATH").isNullOrBlank()) {
                return TOOLBOX
            }

            return null
        }
    }
}