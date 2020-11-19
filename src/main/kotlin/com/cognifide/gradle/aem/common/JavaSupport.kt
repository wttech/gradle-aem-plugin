package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.AemExtension
import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService

class JavaSupport(private val aem: AemExtension) {

    val version = aem.obj.string {
        convention("11")
        aem.prop.string("javaSupport.version")?.let { set(it) }
    }

    val compatibilityVersion = aem.obj.typed<JavaVersion> {
        convention(version.map { JavaVersion.toVersion(it) })
    }

    val languageVersion = aem.obj.typed<JavaLanguageVersion> {
        convention(version.map { JavaLanguageVersion.of(it) })
    }

    val toolchains get() = aem.project.extensions.getByType(JavaToolchainService::class.java)

    val launcher get() = toolchains.launcherFor { it.languageVersion.set(languageVersion) }

    val compiler get() = toolchains.compilerFor { it.languageVersion.set(languageVersion) }
}
