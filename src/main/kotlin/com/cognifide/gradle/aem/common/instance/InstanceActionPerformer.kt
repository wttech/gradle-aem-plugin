package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.action.AbstractAction
import com.cognifide.gradle.aem.common.instance.action.AwaitDownAction
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.action.ReloadAction

/**
 * Executor for actions affecting multiple remote instances at once.
 */
class InstanceActionPerformer(private val aem: AemExtension) {

    fun awaitUp(options: AwaitUpAction.() -> Unit = {}) = action(AwaitUpAction(aem), options)

    fun awaitDown(options: AwaitDownAction.() -> Unit = {}) = action(AwaitDownAction(aem), options)

    fun reload(options: ReloadAction.() -> Unit = {}) = action(ReloadAction(aem), options)

    private fun <T : AbstractAction> action(action: T, configurer: T.() -> Unit) {
        action.apply { notify = false }.apply(configurer).perform()
    }
}