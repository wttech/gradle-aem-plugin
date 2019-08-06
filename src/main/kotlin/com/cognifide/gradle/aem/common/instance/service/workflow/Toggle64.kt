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
    override fun toggle(launcherNode: Node, expected: Boolean) {
        //todo consider parallel running
        when {
            !launcherNode.exists && !expected -> {
                temporaryProps = true
                launcherNode.save(mapOf(
                        "jcr:primaryType" to "cq:WorkflowLauncher",
                        ENABLED_PROP to expected
                /* todo consider eventType property (it's required by launcher view to display workflows properly)
                although not passing it effectively disables the workflow anyway */
                ))
            }
            expected && temporaryProps -> launcherNode.delete()
            expected && !temporaryProps -> launcherNode.saveProperty(ENABLED_PROP, expected)
            launcherNode.exists -> launcherNode.saveProperty(ENABLED_PROP, expected)
        }
    }

    override fun changeRequired(launcher: Node, expected: Boolean): Boolean {
        var state = true
        if (!launcher.exists) {
            // todo fallback to /libs to get the property (get the repository object somehow)
        } else {
            state = launcher.properties[ENABLED_PROP] as Boolean
        }
        return expected != state
    }
}