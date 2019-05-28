package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.action.AbstractAction
import com.cognifide.gradle.aem.common.instance.action.AwaitAction
import com.cognifide.gradle.aem.common.instance.action.ReloadAction
import com.cognifide.gradle.aem.common.instance.action.ShutdownAction

/**
 * Executor for actions affecting multiple remote instances at once.
 */
class InstanceActionPerformer(private val aem: AemExtension) {

    fun await() = await {}

    fun await(options: AwaitAction.() -> Unit) = action(AwaitAction(aem), options)

    fun reload() = reload {}

    fun reload(options: ReloadAction.() -> Unit) = action(ReloadAction(aem), options)

    fun shutdown(options: ShutdownAction.() -> Unit) = action(ShutdownAction(aem), options)

    fun shutdown() = shutdown {}

    private fun <T : AbstractAction> action(action: T, configurer: T.() -> Unit) {
        action.apply { notify = false }.apply(configurer).perform()
    }
}