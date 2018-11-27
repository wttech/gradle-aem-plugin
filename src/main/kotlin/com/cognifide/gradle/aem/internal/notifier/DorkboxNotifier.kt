package com.cognifide.gradle.aem.internal.notifier

import com.cognifide.gradle.aem.base.NotifierFacade
import com.cognifide.gradle.aem.internal.notifier.Notifier as BaseNotifier
import dorkbox.notify.Notify
import javax.imageio.ImageIO
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

class DorkboxNotifier(
    val project: Project,
    val configurer: Notify.() -> Unit
) : BaseNotifier {

    @Suppress("TooGenericExceptionCaught")
    override fun notify(title: String, text: String, level: LogLevel) {
        try {
            Notify.create()
                    .image(ImageIO.read(javaClass.getResourceAsStream(NotifierFacade.IMAGE_PATH)))
                    .apply(configurer)
                    .apply {
                        title(title)
                        text(StringUtils.replace(text, "\n", "<br>"))
                    }
                    .show()
        } catch (e: Exception) {
            project.logger.debug("Cannot show system notification", e)
        }
    }
}