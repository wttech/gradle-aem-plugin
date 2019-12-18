package com.cognifide.gradle.aem.common.notifier

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.BuildScope
import com.fasterxml.jackson.annotation.JsonIgnore
import dorkbox.notify.Notify
import org.gradle.api.logging.LogLevel

class NotifierFacade private constructor(private val aem: AemExtension) {

    /**
     * Turn on/off default system notifications.
     */
    var enabled: Boolean = aem.prop.flag("notifier.enabled")

    /**
     * Hook for customizing notifications being displayed.
     *
     * To customize notification use one of concrete provider methods: 'dorkbox' or 'jcgay' (and optionally pass configuration lambda(s)).
     * Also it is possible to implement own notifier directly in build script by using provider method 'custom'.
     */
    @JsonIgnore
    var config: (NotifierFacade.() -> Notifier) = { dorkbox() }

    private val notifier: Notifier by lazy { config(this@NotifierFacade) }

    fun log(title: String) {
        log(title, "")
    }

    fun log(title: String, message: String) {
        log(title, message, LogLevel.INFO)
    }

    fun log(title: String, message: String, level: LogLevel) {
        aem.logger.log(level, if (message.isNotBlank()) {
            "$title\n$message"
        } else {
            title
        })
    }

    fun notify(title: String) {
        notify(title, "")
    }

    @Suppress("TooGenericExceptionCaught")
    fun notify(title: String, text: String, level: LogLevel, onClick: (Notify) -> Unit = {}) {
        log(title, text, level)

        if (enabled) {
            try {
                notifier.notify(title, text, level, onClick)
            } catch (e: Exception) {
                aem.logger.debug("AEM notifier is not available.", e)
            }
        }
    }

    fun notify(title: String, text: String) = lifecycle(title, text)

    fun dorkbox(configurer: Notify.() -> Unit = {}): Notifier {
        return DorkboxNotifier(aem, configurer)
    }

    fun custom(notifier: (title: String, text: String, level: LogLevel, onClick: (Notify) -> Unit) -> Unit): Notifier {
        return object : Notifier {
            override fun notify(title: String, text: String, level: LogLevel, onClick: (Notify) -> Unit) {
                notifier(title, text, level, onClick)
            }
        }
    }

    fun lifecycle(title: String, text: String) = notify(title, text, LogLevel.LIFECYCLE)

    fun info(title: String, text: String) = notify(title, text, LogLevel.INFO)

    fun warn(title: String, text: String) = notify(title, text, LogLevel.WARN)

    fun error(title: String, text: String) = notify(title, text, LogLevel.ERROR)

    companion object {

        /**
         * Get project specific notifier (config can vary)
         */
        fun of(aem: AemExtension): NotifierFacade {
            return BuildScope.of(aem.project).getOrPut(Notifier::class.java.canonicalName) { setup(aem) }
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
                            val message = result.failure?.message ?: "no error message"

                            notifier.notify("Build failure", message, LogLevel.ERROR)
                        }
                    }
                }
            }

            return notifier
        }
    }
}
