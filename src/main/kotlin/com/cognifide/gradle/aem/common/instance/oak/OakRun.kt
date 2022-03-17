package com.cognifide.gradle.aem.common.instance.oak

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.cli.JarApp
import com.cognifide.gradle.aem.common.instance.LocalInstance
import com.cognifide.gradle.common.utils.using
import java.io.File

class OakRun(val aem: AemExtension, val instance: LocalInstance) {

    private val logger = aem.logger

    val jarApp = JarApp(aem).apply {
        dependencyNotation.apply {
            convention("org.apache.jackrabbit:oak-run:1.36")
            aem.prop.string("oakrun.jar.dependency")?.let { set(it) }
        }
    }

    fun jarApp(options: JarApp.() -> Unit) = jarApp.using(options)

    fun resetPassword(user: String, password: String) {
        try {
            logger.info("Resetting user '$user' password for $instance using OakRun")

            val template = aem.assetManager.readFile("oakrun/admin-reset.groovy.peb").bufferedReader().readText()
            val content = aem.prop.expand(
                template,
                mapOf(
                    "user" to user,
                    "password" to password
                )
            )
            runGroovyScript(content)
        } catch (e: OakRunException) {
            throw OakRunException("Cannot reset password for '$instance'!", e)
        }
    }

    fun runGroovyScript(content: String) {
        val script = OakRunScript(this, content)
        script.exec()
    }

    fun runGroovyScript(file: File) = runGroovyScript(file.bufferedReader().readText())
}
