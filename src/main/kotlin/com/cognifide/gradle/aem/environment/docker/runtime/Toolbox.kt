package com.cognifide.gradle.aem.environment.docker.runtime

import com.cognifide.gradle.aem.AemExtension

class Toolbox(aem: AemExtension) : Base(aem) {

    override val name: String
        get() = NAME

    override val hostIp: String
        get() = aem.props.string("environment.docker.toolbox.hostIp") ?: "192.168.99.100"

    companion object {
        const val NAME = "toolbox"
    }
}
