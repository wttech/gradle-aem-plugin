package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemNotifier
import com.cognifide.gradle.aem.instance.InstanceAction
import org.gradle.api.Project

abstract class AbstractAction(val project: Project) : InstanceAction {

    val config = AemConfig.of(project)

    val notifier = AemNotifier.of(project)

    val logger = project.logger

    var notify = true

    fun notify(title: String, text: String, enabled: Boolean = this.notify) {
        if (enabled) {
            notifier.default(title, text)
        } else {
            notifier.log(title, text)
        }
    }

}