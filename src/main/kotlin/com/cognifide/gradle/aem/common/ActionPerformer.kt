package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.instance.action.AbstractAction
import com.cognifide.gradle.aem.instance.action.AwaitAction
import com.cognifide.gradle.aem.instance.action.ReloadAction

/**
 * Executor for actions affecting multiple instances at once.
 */
class ActionPerformer(val aem: AemExtension) {

    fun await() = await {}

    fun await(configurer: AwaitAction.() -> Unit) = action(AwaitAction(aem), configurer)

    fun reload() = reload {}

    fun reload(configurer: ReloadAction.() -> Unit) = action(ReloadAction(aem), configurer)

    private fun <T : AbstractAction> action(action: T, configurer: T.() -> Unit) {
        action.apply { notify = false }.apply(configurer).perform()
    }
}