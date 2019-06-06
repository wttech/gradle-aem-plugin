package com.cognifide.gradle.aem.environment.docker.base.type

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.environment.docker.base.DockerType

abstract class Base(protected val aem: AemExtension) : DockerType {

    override fun toString(): String = name.toLowerCase()
}