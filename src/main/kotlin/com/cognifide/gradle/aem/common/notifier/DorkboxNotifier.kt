package com.cognifide.gradle.aem.common.notifier

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.notifier.Notifier as BaseNotifier
import dorkbox.notify.Notify
import dorkbox.notify.Theme
import org.apache.commons.lang3.StringUtils
import org.gradle.api.logging.LogLevel
import java.awt.Color
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

class DorkboxNotifier(
    val aem: AemExtension,
    val configurer: Notify.() -> Unit
) : BaseNotifier {

    @Suppress("TooGenericExceptionCaught")
    override fun notify(title: String, text: String, level: LogLevel, onClick: (Notify) -> Unit) {
        try {
            Notify.create().apply {
                text(DORKBOX_DARK_LIGHT_THEME)
                hideAfter(TimeUnit.SECONDS.toMillis(DORKBOX_HIDE_AFTER_SECONDS).toInt())
                assignImage(level)

                configurer()

                title(title)
                text(StringUtils.replace(text, "\n", "<br>"))
                onAction(onClick)

                show()
            }
        } catch (e: Exception) {
            aem.logger.debug("Cannot show system notification", e)
        }
    }

    private fun Notify.assignImage(level: LogLevel) {
        when (level) {
            LogLevel.WARN -> typedImage("error.png")
            LogLevel.ERROR -> typedImage("error.png")
            else -> if (!projectSpecificImage()) typedImage("success.png")
        }
    }

    private fun Notify.typedImage(type: String) {
        image(ImageIO.read(javaClass.getResource("/com/cognifide/gradle/aem/common/notifier/$type").toURI().toURL()))
    }

    private fun Notify.projectSpecificImage(): Boolean {
        val image = aem.project.file("${aem.packageOptions.metaCommonDir}/vault/definition/thumbnail.png")
        if (!image.exists()) {
            return false
        }

        image(ImageIO.read(image.toURI().toURL()))

        return true
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
    }
}
