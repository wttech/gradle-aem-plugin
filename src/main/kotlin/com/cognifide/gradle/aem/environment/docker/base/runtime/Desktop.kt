package com.cognifide.gradle.aem.environment.docker.base.runtime

import com.cognifide.gradle.aem.AemExtension

class Desktop(aem: AemExtension) : Base(aem) {

    override val name: String
        get() = NAME

    override val hostIp: String
        get() = aem.props.string("environment.docker.desktop.hostIp") ?: "127.0.0.1"

    companion object {
        const val NAME = "desktop"
    }
}