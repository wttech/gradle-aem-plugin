package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.internal.notifier.DorkboxNotifier
import com.cognifide.gradle.aem.internal.notifier.JcGayNotifier
import com.cognifide.gradle.aem.internal.notifier.Notifier
import dorkbox.notify.Notify
import fr.jcgay.notification.Notification
import org.apache.commons.lang3.exception.ExceptionUtils
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

class AemNotifier private constructor(private val project: Project) {

    private val config by lazy { AemConfig.of(project) }

    private lateinit var notifier: Notifier

    init {
        dorkbox()
    }

    fun log(title: String) {
        log(title, "")
    }

    fun log(title: String, message: String) {
        log(title, message, LogLevel.INFO)
    }

    fun log(title: String, message: String, level: LogLevel) {
        project.logger.log(level, if (message.isNotBlank()) {
            "${title.removeSuffix(".")}. $message"
        } else {
            title
        })
    }

    fun default(title: String) {
        default(title, "")
    }

    fun default(title: String, message: String) {
        default(title, message, LogLevel.INFO)
    }

    fun default(title: String, message: String, level: LogLevel) {
        if (config.notificationEnabled) {
            notify(title, message, level)
        }

        log(title, message, level)
    }

    fun notify(title: String) {
        notify(title, "")
    }

    fun notify(title: String, text: String) {
        notify(title, text, LogLevel.INFO)
    }

    fun notify(title: String, text: String, level: LogLevel) {
        notifier.notify(title, text, level)
    }

    fun dorkbox() {
        dorkbox { darkStyle().hideAfter(5000) }
    }

    fun dorkbox(configurer: Notify.() -> Unit) {
        notifier = DorkboxNotifier(project, configurer)
    }

    fun jcgay() {
        jcgay {}
    }

    fun jcgay(configurer: Notification.Builder.() -> Unit) {
        notifier = JcGayNotifier(project, configurer)
    }

    companion object {

        const val IMAGE_PATH = "/com/cognifide/gradle/aem/META-INF/vault/definition/thumbnail.png"

        const val EXT_INSTANCE_PROP = "aemNotifier"

        val LOG_LEVEL_NOTIFY_MAP: HashMap<LogLevel, Notification.Level> = hashMapOf(
                LogLevel.ERROR to Notification.Level.ERROR,
                LogLevel.WARN to Notification.Level.WARNING
        )

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

                        notifier.default("Build failure", "$message\n$exception", LogLevel.ERROR)
                    }
                }
            }

            return notifier
        }

    }

}