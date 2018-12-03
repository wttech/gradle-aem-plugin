package com.cognifide.gradle.aem.base

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.BuildScope
import com.cognifide.gradle.aem.common.notifier.DorkboxNotifier
import com.cognifide.gradle.aem.common.notifier.JcGayNotifier
import com.cognifide.gradle.aem.common.notifier.Notifier
import dorkbox.notify.Notify
import fr.jcgay.notification.Application
import fr.jcgay.notification.Notification
import java.util.concurrent.TimeUnit
import org.apache.commons.lang3.exception.ExceptionUtils
import org.gradle.api.logging.LogLevel

class NotifierFacade private constructor(private val aem: AemExtension) {

    private val notifier: Notifier by lazy { aem.config.notificationConfig(this@NotifierFacade) }

    fun log(title: String) {
        log(title, "")
    }

    fun log(title: String, message: String) {
        log(title, message, LogLevel.INFO)
    }

    fun log(title: String, message: String, level: LogLevel) {
        aem.logger.log(level, if (message.isNotBlank()) {
            "${title.removeSuffix(".")}. $message"
        } else {
            title
        })
    }

    fun notify(title: String) {
        notify(title, "")
    }

    fun notify(title: String, text: String) {
        notify(title, text, LogLevel.INFO)
    }

    @Suppress("TooGenericExceptionCaught")
    fun notify(title: String, text: String, level: LogLevel) {
        log(title, text, level)

        try {
            if (aem.config.notificationEnabled) {
                notifier.notify(title, text, level)
            }
        } catch (e: Exception) {
            aem.logger.debug("AEM notifier is not available.", e)
        }
    }

    fun dorkbox(): Notifier {
        return dorkbox { darkStyle().hideAfter(TimeUnit.SECONDS.toMillis(DORKBOX_HIDE_AFTER_SECONDS).toInt()) }
    }

    fun dorkbox(configurer: Notify.() -> Unit): Notifier {
        return DorkboxNotifier(aem.project, configurer)
    }

    fun jcgay(): JcGayNotifier {
        return jcgay({ timeout(TimeUnit.SECONDS.toMillis(JCGAY_TIMEOUT_SECONDS)) }, {})
    }

    fun jcgay(appBuilder: Application.Builder.() -> Unit, messageBuilder: Notification.Builder.() -> Unit): JcGayNotifier {
        return JcGayNotifier(aem.project, appBuilder, messageBuilder)
    }

    fun custom(notifier: (title: String, text: String, level: LogLevel) -> Unit): Notifier {
        return object : Notifier {
            override fun notify(title: String, text: String, level: LogLevel) {
                notifier(title, text, level)
            }
        }
    }

    fun byType(type: Type): Notifier {
        return when (type) {
            Type.DORKBOX -> dorkbox()
            Type.JCGAY -> jcgay()
        }
    }

    enum class Type {
        DORKBOX,
        JCGAY;

        companion object {
            fun of(name: String): Type {
                return values().find { it.name.equals(name, true) }
                        ?: throw AemException("Unsupported notification type: $name")
            }
        }
    }

    companion object {

        const val IMAGE_PATH = "/com/cognifide/gradle/aem/META-INF/vault/definition/thumbnail.png"

        const val JCGAY_TIMEOUT_SECONDS = 5L

        const val DORKBOX_HIDE_AFTER_SECONDS = 5L

        /**
         * Get project specific notifier (config can vary)
         */
        fun of(aem: AemExtension): NotifierFacade {
            return BuildScope.of(aem.project).getOrPut(Notifier::class.java.canonicalName, { setup(aem) })
        }

        /**
         * Register once (for root project only) listener for notifying about build errors.
         */
        private fun setup(aem: AemExtension): NotifierFacade {
            val notifier = NotifierFacade(aem)

            if (aem.project == aem.project.rootProject) {
                aem.project.gradle.buildFinished { result ->
                    if (result.failure != null) {
                        val exception = ExceptionUtils.getRootCause(result.failure)
                        val message = exception?.message ?: "no error message"

                        notifier.notify("Build failure", message, LogLevel.ERROR)
                    }
                }
            }

            return notifier
        }
    }
}