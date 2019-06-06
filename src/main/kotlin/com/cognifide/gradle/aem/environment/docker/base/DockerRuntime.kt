package com.cognifide.gradle.aem.environment.docker.base

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.environment.docker.base.runtime.Desktop
import com.cognifide.gradle.aem.environment.docker.base.runtime.Toolbox

interface DockerRuntime {

    val name: String

    val hostIp: String

    companion object {

        fun determine(aem: AemExtension): DockerRuntime {
            return aem.props.string("environment.docker.type")?.let { of(aem, it) } ?: detect(aem) ?: Desktop(aem)
        }

        fun of(aem: AemExtension, name: String): DockerRuntime? = when (name.toLowerCase()) {
            Toolbox.NAME -> Toolbox(aem)
            Desktop.NAME -> Desktop(aem)
            else -> throw DockerException("Unsupported Docker type '$name'")
        }

        fun detect(aem: AemExtension): DockerRuntime? {
            if (!System.getenv("DOCKER_TOOLBOX_INSTALL_PATH").isNullOrBlank()) {
                return Toolbox(aem)
            }

            return null
        }
    }
}