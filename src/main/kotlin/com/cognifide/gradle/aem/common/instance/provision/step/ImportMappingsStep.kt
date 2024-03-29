package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.provision.ProvisionException
import com.cognifide.gradle.aem.common.instance.provision.Provisioner
import com.cognifide.gradle.aem.common.instance.provision.Step

class ImportMappingsStep(provisioner: Provisioner, val fileName: String) : Step(provisioner) {

    val jsonFile get() = provisioner.manager.configDir.get().asFile.resolve("mapping/$fileName")

    val root = aem.obj.string {
        convention("/etc/map/http")
    }

    override fun validate() {
        if (!jsonFile.exists()) {
            throw ProvisionException("Mapping file to be imported does not exist '$jsonFile'!")
        }
    }

    override fun action(instance: Instance) {
        instance.sync {
            repository.import(root.get(), jsonFile)
        }
    }

    init {
        id.set("importMappings/$fileName")
        description.convention("Importing mappings file '$fileName'")
        version.set(aem.obj.provider { versionFrom(jsonFile, root.get()) })
        condition { onPublish() && once() }
    }
}
