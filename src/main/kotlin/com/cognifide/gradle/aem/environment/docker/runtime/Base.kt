package com.cognifide.gradle.aem.environment.docker.runtime

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.environment.docker.Runtime

abstract class Base(protected val aem: AemExtension) : Runtime {

    override fun toString(): String = name.toLowerCase()
}
