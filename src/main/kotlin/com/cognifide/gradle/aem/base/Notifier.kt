package com.cognifide.gradle.aem.base

import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.internal.BuildScope
import com.cognifide.gradle.aem.internal.notifier.DorkboxNotifier
import com.cognifide.gradle.aem.internal.notifier.JcGayNotifier
import com.cognifide.gradle.aem.internal.notifier.Notifier
import dorkbox.notify.Notify
import fr.jcgay.notification.Application
import fr.jcgay.notification.Notification
import java.util.concurrent.TimeUnit
import org.apache.commons.lang3.exception.ExceptionUtils
import org.gradle.api.logging.LogLevel

class Notifier private constructor(private val aem: BaseExtension) {

    private val notifier: Notifier by lazy { aem.config.notificationConfig(this@Notifier) }

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

    fun notify(title: String, text: String, level: LogLevel) {
        log(title, text, level)

        try {
            if (aem.config.notificationEnabled) {
                notifier.notify(title, text, level)
            }
        } catch (e: Throwable) {
            aem.logger.debug("AEM notifier is not available.", e)
        }
    }

    fun dorkbox(): Notifier {
        return dorkbox { darkStyle().hideAfter(TimeUnit.SECONDS.toMillis(5).toInt()) }
    }

    fun dorkbox(configurer: Notify.() -> Unit): Notifier {
        return DorkboxNotifier(aem.project, configurer)
    }

    fun jcgay(): JcGayNotifier {
        return jcgay({ timeout(TimeUnit.SECONDS.toMillis(5)) }, {})
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

    fun factory(): Notifier {
        val name = aem.props.string("aem.notification.config", "dorkbox")

        return when (name) {
            "dorkbox" -> dorkbox()
            "jcgay" -> jcgay()
            else -> throw AemException("Unsupported notifier: '$name'")
        }
    }

    companion object {

        const val IMAGE_PATH = "/com/cognifide/gradle/aem/META-INF/vault/definition/thumbnail.png"

        /**
         * Get project specific notifier (config can vary)
         */
        fun of(aem: BaseExtension): com.cognifide.gradle.aem.base.Notifier {
            return BuildScope.of(aem.project).getOrPut(Notifier::class.java.canonicalName, { setup(aem) })
        }

        /**
         * Register once (for root project only) listener for notifying about build errors.
         */
        private fun setup(aem: BaseExtension): com.cognifide.gradle.aem.base.Notifier {
            val notifier = Notifier(aem)

            if (aem.project == aem.project.rootProject) {
                aem.project.gradle.buildFinished {
                    if (it.failure != null) {
                        val exception = ExceptionUtils.getRootCause(it.failure)
                        val message = exception?.message ?: "no error message"

                        notifier.notify("Build failure", message, LogLevel.ERROR)
                    }
                }
            }

            return notifier
        }
    }
}