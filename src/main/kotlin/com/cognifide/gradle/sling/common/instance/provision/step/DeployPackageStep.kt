package com.cognifide.gradle.sling.common.instance.provision.step

import com.cognifide.gradle.sling.common.instance.Instance
import com.cognifide.gradle.sling.common.instance.provision.Provisioner
import com.cognifide.gradle.common.utils.toLowerCamelCase

class DeployPackageStep(provisioner: Provisioner) : AbstractStep(provisioner) {

    val source = sling.obj.typed<Any>()

    val sourceProperties by lazy { DeployPackageSource.from(source.get(), sling) }

    val file by lazy { sling.packageOptions.wrapper.wrap(provisioner.fileResolver.get(source.get()).file) }

    val name = sling.obj.string { convention(sling.obj.provider { sourceProperties.name }) }

    override fun init() {
        logger.debug("Resolved package '${name.get()}' to be deployed is located at path: '$file'")
    }

    override fun action(instance: Instance) = instance.sync {
        logger.info("Deploying package '${name.get()}' to $instance")
        awaitIf { packageManager.deploy(file) }
    }

    fun isDeployedOn(instance: Instance) = instance.sync.packageManager.isDeployed(file)

    fun notDeployedOn(instance: Instance) = !isDeployedOn(instance)

    init {
        id.convention(name.map { "deployPackage/${it.toLowerCamelCase()}" })
        description.convention(name.map { "Deploying package '${name.get()}'" })
        version.convention(sling.obj.provider { sourceProperties.version })

        if (sling.prop.boolean("instance.provision.deployPackage.strict") == true) {
            condition { notDeployedOn(instance) }
        }
    }
}
