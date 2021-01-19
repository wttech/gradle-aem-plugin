package com.cognifide.gradle.aem.common.java

import com.cognifide.gradle.aem.AemExtension
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.JavaCompiler
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.internal.DefaultJavaToolchainService
import org.gradle.jvm.toolchain.internal.JavaToolchainQueryService

class JavaSupport(private val aem: AemExtension) {

    private val logger = aem.project.logger

    val version = aem.obj.string {
        convention("11")
        aem.prop.string("javaSupport.version")?.let { set(it) }
    }

    val fallback = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("javaSupport.fallback")?.let { set(it) }
    }

    val compatibilityVersion = aem.obj.typed<JavaVersion> {
        convention(version.map { JavaVersion.toVersion(it) })
    }

    val languageVersion = aem.obj.typed<JavaLanguageVersion> {
        convention(version.map { JavaLanguageVersion.of(it) })
    }

    val toolchainQuery by lazy { aem.common.services.get<JavaToolchainQueryService>() }

    val toolchainDefault by lazy {
        if (aem.project.plugins.hasPlugin(JavaBasePlugin::class.java)) {
            aem.project.extensions.getByType(JavaPluginExtension::class.java).toolchain
        } else {
            throw JavaException("Cannot access default Java toolchain! Project '${aem.project.path}' is not applying Java base plugin.")
        }
    }

    val toolchainService by lazy {
        if (aem.project.plugins.hasPlugin(JavaBasePlugin::class.java)) {
            aem.project.extensions.getByType(JavaToolchainService::class.java)
        } else {
            aem.project.extensions.create(JavaToolchainService::class.java, TOOLCHAINS_EXTENSION,
                    DefaultJavaToolchainService::class.java, toolchainQuery
            )
        }
    }

    val launcher: Provider<JavaLauncher>
        get() = try {
            toolchainService.launcherFor { it.languageVersion.set(languageVersion) }
        } catch (e: GradleException) {
            if (fallback.get()) {
                logger.warn("Using fallback Java launcher!")
                logger.debug("Cannot determine Java launcher using toolchains service!", e)
                toolchainService.launcherFor(toolchainDefault)
            } else {
                throw JavaException("Cannot determine Java launcher via toolchains!", e)
            }
        }

    val launcherPath get() = launcher.get().executablePath.asFile.absolutePath

    val compiler: Provider<JavaCompiler>
        get() = try {
            toolchainService.compilerFor { it.languageVersion.set(languageVersion) }
        } catch (e: GradleException) {
            if (fallback.get()) {
                logger.warn("Using fallback Java compiler!")
                logger.debug("Cannot determine Java compiler using toolchains service!", e)
                toolchainService.compilerFor(toolchainDefault)
            } else {
                throw JavaException("Cannot determine Java compiler via toolchains!", e)
            }
        }

    val compilerPath get() = compiler.get().executablePath.asFile.absolutePath

    companion object {
        const val TOOLCHAINS_EXTENSION = "aemJavaToolchains"
    }
}
