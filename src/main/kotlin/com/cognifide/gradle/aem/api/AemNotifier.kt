package com.cognifide.gradle.aem.api

import dorkbox.notify.Notify
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.gradle.api.Project
import javax.imageio.ImageIO

class AemNotifier private constructor(private val project: Project) {

    private val config by lazy {AemConfig.of(project) }

    fun default(title: String, message: String) {
        if (config.notifications) {
            now(title, message)
        }
    }

    // TODO allow to customize color of plugin logo (warn, info, error etc)
    fun now(title: String, text: String) {
        now {
            title(title)
            text(StringUtils.replace(text, "\n", "<br>"))
        }
    }

    fun now(configurer: Notify.() -> Unit) {
        try {
            Notify.create()
                    .title(AemPlugin.NAME_WITH_VERSION)
                    .image(ImageIO.read(javaClass.getResourceAsStream(IMAGE_PATH)))
                    .darkStyle()
                    .hideAfter(5000)
                    .apply(configurer)
                    .show()
        } catch (e: Exception) {
            project.logger.debug("Cannot show system notification", e)
        }
    }

    companion object {

        const val IMAGE_PATH = "/com/cognifide/gradle/aem/META-INF/vault/definition/thumbnail.png"

        fun of(project: Project): AemNotifier {
            val rootProject = project.rootProject
            val name = AemNotifier::class.java.canonicalName

            val props = rootProject.extensions.extraProperties
            if (!props.has(name)) {
                props.set(name, setup(rootProject))

            }

            return props.get(name) as AemNotifier
        }

        private fun setup(rootProject: Project): AemNotifier {
            val notifier = AemNotifier(rootProject)
            rootProject.gradle.buildFinished {
                if (it.failure != null) {
                    val exception = ExceptionUtils.getRootCause(it.failure)
                    val message = exception?.message ?: "no message available"
                    notifier.now("Build failure",  "$message\nplugin version: ${AemPlugin.BUILD.version}")
                }
            }

            return notifier
        }

    }

}