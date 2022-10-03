package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.provision.ProvisionException
import com.cognifide.gradle.aem.common.instance.provision.Provisioner
import com.cognifide.gradle.aem.common.instance.provision.Step

class ConfigureCryptoStep(provisioner: Provisioner) : Step(provisioner) {

    val fileSymbolicName = aem.obj.string {
        convention("com.adobe.granite.crypto.file")
        aem.prop.string("instance.provision.configureCrypto.fileSymbolicName")?.let { set(it) }
    }

    val hmac = aem.obj.typed<Any>()

    val hmacFile by lazy { provisioner.fileResolver.get(hmac.get()).file }

    val master = aem.obj.typed<Any>()

    val masterFile by lazy { provisioner.fileResolver.get(master.get()).file }

    override fun validate() {
        if (!hmac.isPresent) {
            throw ProvisionException("Crypto HMAC file source is not defined!")
        }
        if (!master.isPresent) {
            throw ProvisionException("Crypto Master file source is not defined!")
        }
    }

    override fun init() {
        logger.debug(
            listOf(
                "Resolved Crypto files are located at paths:",
                "HMAC: $hmacFile",
                "Master: $masterFile"
            ).joinToString("\n")
        )
    }

    override fun action(instance: Instance) = instance.sync {
        instance.local {
            logger.info("Configuring Crypto started for $instance")

            val fileBundle = osgi.getBundle(fileSymbolicName.get())
            val dataDir = fileBundle.dir.resolve("data")

            logger.info(
                listOf(
                    "Copying Crypto files to directory $dataDir:",
                    "HMAC: $hmacFile",
                    "Master: $masterFile"
                ).joinToString("\n")
            )
            hmacFile.copyTo(dataDir.resolve("hmac"), true)
            masterFile.copyTo(dataDir.resolve("master"), true)

            osgi.restart()

            logger.info("Configuring Crypto finished for $instance")
        }
    }

    init {
        id.set("configureCrypto")
        description.convention("Configuring Crypto")
    }
}
