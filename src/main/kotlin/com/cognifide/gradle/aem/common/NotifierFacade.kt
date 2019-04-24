package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.common.notifier.DorkboxNotifier
import com.cognifide.gradle.aem.common.notifier.Notifier
import dorkbox.notify.Notify
import dorkbox.notify.Theme
import java.awt.Color
import java.awt.Desktop
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
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
        return dorkbox {
            text(DORKBOX_DARK_LIGHT_THEME)
                    .hideAfter(TimeUnit.SECONDS.toMillis(DORKBOX_HIDE_AFTER_SECONDS).toInt())
                    .image(ImageIO.read(image))
        }
    }

    fun dorkbox(configurer: Notify.() -> Unit): Notifier {
        return DorkboxNotifier(aem, configurer)
    }

    fun custom(notifier: (title: String, text: String, level: LogLevel, onClick: (Notify) -> Unit) -> Unit): Notifier {
        return object : Notifier {
            override fun notify(title: String, text: String, level: LogLevel, onClick: (Notify) -> Unit) {
                notifier(title, text, level, onClick)
            }
        }
    }

    fun notifyLogError(title: String, message: String, file: URI) {
        notifier.notify(title, message, LogLevel.INFO) {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(file)
            }
        }
    }

    val image: URL
        get() {
            val customThumbnail = aem.project.file("${aem.config.packageMetaCommonRoot}/vault/definition/thumbnail.png")
            return if (customThumbnail.exists()) {
                customThumbnail.toURI().toURL()
            } else {
                javaClass.getResource("/com/cognifide/gradle/aem/META-INF/vault/definition/thumbnail.png")
            }
        }

    companion object {

        const val DORKBOX_HIDE_AFTER_SECONDS = 5L

        val DORKBOX_DARK_LIGHT_THEME = Theme(
                Notify.TITLE_TEXT_FONT,
                Notify.MAIN_TEXT_FONT,
                Color.DARK_GRAY,
                Color(168, 168, 168),
                Color(220, 220, 220),
                Color(220, 220, 220),
                Color.GRAY
        )

        /**
         * Get project specific notifier (config can vary)
         */
        fun of(aem: AemExtension): NotifierFacade {
            return BuildScope.of(aem.project).getOrPut(Notifier::class.java.canonicalName, { setup(aem) })
        }

        /**
         * Register once (for root project only) listener for notifying about build errors (if any task executed).
         */
        private fun setup(aem: AemExtension): NotifierFacade {
            val notifier = NotifierFacade(aem)
            if (aem.project != aem.project.rootProject) {
                return notifier
            }

            aem.project.gradle.taskGraph.whenReady { graph ->
                if (graph.allTasks.isNotEmpty()) {
                    aem.project.gradle.buildFinished { result ->
                        if (result.failure != null) {
                            val exception = ExceptionUtils.getRootCause(result.failure)
                            val message = exception?.message ?: "no error message"

                            notifier.notify("Build failure", message, LogLevel.ERROR)
                        }
                    }
                }
            }

            return notifier
        }
    }
}