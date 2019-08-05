package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.instance.service.workflow.ToggleStrategy.Companion.ENABLED_PROP

class Toggle64 : ToggleStrategy {

    /**
     * variable determines if saved node should be deleted after given action
     * (if node does not exist under launcher path, props are taken from /libs)
     */
    private var temporaryProps = false

    /**
     * there are 4 states workflow can be in (AEM 6.4 and above):
     * - launcher node exists -> config is custom so  just overwrite the property
     * - launcher node does not exist -> if enabling, create a temporary node
     * - workflow is to be disabled - if it was a temporary node, delete it
     * - workflow is to be disabled - if it was not a temporary node, change property
     */
    override fun toggle(launcherNode: Node, state: Boolean) {
        when {
            launcherNode.exists -> launcherNode.saveProperty(ENABLED_PROP, state)
            !launcherNode.exists && state -> {
                temporaryProps = true
                launcherNode.save(mapOf(
                        "jcr:primaryType" to "cq:WorkflowLauncher",
                        ENABLED_PROP to state
                ))
            }
            !state && temporaryProps -> launcherNode.delete()
            !state && !temporaryProps -> launcherNode.saveProperty(ENABLED_PROP, state)
        }
    }
}