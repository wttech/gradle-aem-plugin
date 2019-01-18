package com.cognifide.gradle.aem.common.notifier

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.notifier.Notifier as BaseNotifier
import dorkbox.notify.Notify
import org.apache.commons.lang3.StringUtils
import org.gradle.api.logging.LogLevel

class DorkboxNotifier(
    val aem: AemExtension,
    val configurer: Notify.() -> Unit
) : BaseNotifier {

    @Suppress("TooGenericExceptionCaught")
    override fun notify(title: String, text: String, level: LogLevel, clickActionHandler: (Notify) -> Unit) {
        try {
            Notify.create()
                    .apply(configurer)
                    .apply {
                        title(title)
                        text(StringUtils.replace(text, "\n", "<br>"))
                        onAction(clickActionHandler)
                    }
                    .show()
        } catch (e: Exception) {
            aem.logger.debug("Cannot show system notification", e)
        }
    }
}