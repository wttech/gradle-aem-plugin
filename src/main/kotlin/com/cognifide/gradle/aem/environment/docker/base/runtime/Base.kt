package com.cognifide.gradle.aem.environment.docker.base.runtime

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.environment.docker.base.DockerRuntime

abstract class Base(protected val aem: AemExtension) : DockerRuntime {

    override fun toString(): String = name.toLowerCase()
}
