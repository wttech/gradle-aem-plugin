package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.environment.docker.runtime.Desktop
import com.cognifide.gradle.aem.environment.docker.runtime.Toolbox
import java.io.File

interface Runtime {

    val name: String

    val hostIp: String

    val safeVolumes: Boolean

    /**
     * @see <https://github.com/docker/for-linux/issues/264>
     */
    val definedHostInternal: Boolean

    fun determinePath(path: String): String

    fun determinePath(file: File) = determinePath(file.toString())

    companion object {

        fun determine(aem: AemExtension): Runtime {
            return aem.props.string("environment.docker.type")?.let { of(aem, it) } ?: detect(aem) ?: Desktop(aem)
        }

        fun of(aem: AemExtension, name: String): Runtime? = when (name.toLowerCase()) {
            Toolbox.NAME -> Toolbox(aem)
            Desktop.NAME -> Desktop(aem)
            else -> throw DockerException("Unsupported Docker type '$name'")
        }

        fun detect(aem: AemExtension): Runtime? {
            if (!System.getenv("DOCKER_TOOLBOX_INSTALL_PATH").isNullOrBlank()) {
                return Toolbox(aem)
            }

            return null
        }
    }
}
