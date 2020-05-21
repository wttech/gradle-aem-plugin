package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.provision.Provisioner

class DeployPackageStep(provisioner: Provisioner, val name: String, val url: Any) : AbstractStep(provisioner, "deployPackage/${slug(name)}") {

    val pkg by lazy {
        val file = provisioner.fileResolver.get(url).file
        aem.packageOptions.wrapper.wrap(file)
    }

    override fun init() {
        logger.info("Resolved package '$name' to be deployed is located at path: '$pkg'")
    }

    override fun action(instance: Instance) = instance.sync {
        logger.info("Deploying package '$name' to $instance")
        awaitIf { packageManager.deploy(pkg) }
    }

    fun isDeployedOn(instance: Instance) = instance.sync.packageManager.isDeployed(pkg)

    fun notDeployedOn(instance: Instance) = !isDeployedOn(instance)

    init {
        description.set("Deploying package '$name'")

        if (aem.prop.boolean("instance.provision.deployPackage.strict") == true) {
            condition { notDeployedOn(instance) }
        }
    }

    companion object {
        private fun slug(name: String) = name.replace(".", "-").replace(":", "_")
    }
}
