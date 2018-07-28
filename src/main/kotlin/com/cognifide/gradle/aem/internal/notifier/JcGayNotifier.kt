package com.cognifide.gradle.aem.internal.notifier

import com.cognifide.gradle.aem.api.AemNotifier
import com.cognifide.gradle.aem.api.AemPlugin
import fr.jcgay.notification.Application
import fr.jcgay.notification.Icon
import fr.jcgay.notification.Notification
import fr.jcgay.notification.SendNotification
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import java.util.concurrent.TimeUnit

class JcGayNotifier(val project: Project, val configurer: Notification.Builder.() -> Unit) : Notifier {

    private val icon by lazy { Icon.create(javaClass.getResource(AemNotifier.IMAGE_PATH), "default") }

    private val application by lazy {
        Application.builder()
                .id(AemPlugin.ID)
                .name(AemPlugin.NAME)
                .icon(icon)
                .timeout(TimeUnit.SECONDS.toMillis(5))
                .build()
    }

    override fun notify(title: String, text: String, level: LogLevel) {
        val notifier = SendNotification()
                .setApplication(application)
                .initNotifier()
        val notification = Notification.builder()
                .icon(icon)
                .apply(configurer)
                .title(title)
                .message(text)
                .level(AemNotifier.LOG_LEVEL_NOTIFY_MAP[level] ?: Notification.Level.INFO)
                .build()
        try {
            notifier.send(notification)
        } catch (e: Exception) {
            project.logger.debug("Cannot show system notification", e)
        } finally {
            notifier.close()
        }
    }
}