package com.cognifide.gradle.aem.internal.notifier

import com.cognifide.gradle.aem.api.AemNotifier
import com.cognifide.gradle.aem.api.AemPlugin
import fr.jcgay.notification.Application
import fr.jcgay.notification.Icon
import fr.jcgay.notification.Notification
import fr.jcgay.notification.SendNotification
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

class JcGayNotifier(
        val project: Project,
        val appBuilder: Application.Builder.() -> Unit,
        val messageBuilder: Notification.Builder.() -> Unit
) : Notifier {

    private val icon by lazy {
        Icon.create(javaClass.getResource(AemNotifier.IMAGE_PATH), "default")
    }

    private val notifier by lazy {
        SendNotification()
                .setApplication(Application.builder()
                        .id(AemPlugin.ID)
                        .name(AemPlugin.NAME)
                        .icon(icon)
                        .apply(appBuilder)
                        .build())
                .initNotifier()
    }

    override fun notify(title: String, text: String, level: LogLevel) {
        try {
            val notification = Notification.builder()
                    .icon(icon)
                    .apply(messageBuilder)
                    .title(title)
                    .message(text)
                    .level(LOG_LEVEL_NOTIFY_MAP[level] ?: Notification.Level.INFO)
                    .build()
            notifier.send(notification)
        } catch (e: Exception) {
            project.logger.debug("Cannot show system notification", e)
        }
    }

    companion object {

        val LOG_LEVEL_NOTIFY_MAP = mapOf(
                LogLevel.ERROR to Notification.Level.ERROR,
                LogLevel.WARN to Notification.Level.WARNING
        )

    }
}