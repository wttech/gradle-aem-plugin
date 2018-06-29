package com.cognifide.gradle.aem.api

import fr.jcgay.notification.*
import org.apache.commons.lang3.exception.ExceptionUtils
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import java.util.concurrent.TimeUnit

class AemNotifier private constructor(private val project: Project) {

    enum class Level {
        ERROR, WARNING, INFO
    }

    private val config by lazy { AemConfig.of(project) }
    private val icon by lazy { Icon.create(javaClass.getResource(IMAGE_PATH), "default") }
    private val application by lazy {
        Application.builder()
                .id("gradle-aem-plugin")
                .name("Gradle AEM Plugin")
                .icon(icon)
                .timeout(TimeUnit.SECONDS.toMillis(5))
                .build()
    }
    private val levelToLog: HashMap<Level, LogLevel> = hashMapOf(
            Level.ERROR to LogLevel.ERROR,
            Level.WARNING to LogLevel.WARN,
            Level.INFO to LogLevel.LIFECYCLE)
    private val levelToNotify: HashMap<Level, Notification.Level> = hashMapOf(
            Level.ERROR to Notification.Level.ERROR,
            Level.WARNING to Notification.Level.WARNING,
            Level.INFO to Notification.Level.INFO)

    fun log(title: String, message: String, level: Level = Level.INFO) {
        val logLevel = levelToLog.getOrDefault(level, LogLevel.LIFECYCLE)
        project.logger.log(logLevel, if (message.isNotBlank()) {
            "${title.removeSuffix(".")}. $message"
        } else {
            title
        })
    }

    fun default(title: String, message: String = "", level: Level = Level.INFO) {
        if (config.notificationEnabled) {
            notify(title, message, level)
        }

        log(title, message, level)
    }

    fun notify(title: String, text: String = "", level: Level = Level.INFO) {
        val notifier = SendNotification()
                .setApplication(application)
                .initNotifier()
        val notification = Notification.builder()
                .title(title)
                .message(text)
                .icon(icon)
                .level(levelToNotify.getOrDefault(level, Notification.Level.INFO))
                .build()
        try {
            notifier.send(notification)
        } catch (e: Exception) {
            project.logger.debug("Cannot show system notification", e)
        } finally {
            notifier.close()
        }
    }

    companion object {

        const val IMAGE_PATH = "/com/cognifide/gradle/aem/META-INF/vault/definition/thumbnail.png"

        const val EXT_INSTANCE_PROP = "aemNotifier"

        /**
         * Get project specific notifier (config can vary)
         */
        fun of(project: Project): AemNotifier {
            val props = project.extensions.extraProperties
            if (!props.has(EXT_INSTANCE_PROP)) {
                props.set(EXT_INSTANCE_PROP, setup(project))
            }

            return props.get(EXT_INSTANCE_PROP) as AemNotifier
        }

        /**
         *
         */
        private fun setup(project: Project): AemNotifier {
            val notifier = AemNotifier(project)

            if (project == project.rootProject) {
                project.gradle.buildFinished {
                    if (it.failure != null) {
                        val exception = ExceptionUtils.getRootCause(it.failure)
                        val message = exception?.message ?: "no error message"

                        notifier.default("Build failure", "${message}\n${exception}", Level.ERROR)
                    }
                }
            }

            return notifier
        }

    }

}