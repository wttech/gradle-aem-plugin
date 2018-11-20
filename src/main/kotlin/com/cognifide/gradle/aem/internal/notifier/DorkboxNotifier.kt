package com.cognifide.gradle.aem.internal.notifier

import com.cognifide.gradle.aem.base.Notifier
import dorkbox.notify.Notify
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import javax.imageio.ImageIO
import com.cognifide.gradle.aem.internal.notifier.Notifier as BaseNotifier

class DorkboxNotifier(
        val project: Project,
        val configurer: Notify.() -> Unit
) : BaseNotifier {

    override fun notify(title: String, text: String, level: LogLevel) {
        try {
            Notify.create()
                    .image(ImageIO.read(javaClass.getResourceAsStream(Notifier.IMAGE_PATH)))
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