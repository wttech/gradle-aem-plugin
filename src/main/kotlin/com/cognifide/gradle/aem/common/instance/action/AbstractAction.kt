package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.InstanceAction

abstract class AbstractAction(protected val aem: AemExtension) : InstanceAction {

    protected val common = aem.common

    protected val logger = aem.logger

    var enabled = true

    var notify = true

    fun notify(title: String, text: String, enabled: Boolean = this.notify) {
        if (enabled) {
            common.notifier.notify(title, text)
        } else {
            common.notifier.log(title, text)
        }
    }
}
