package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.provision.Provisioner

class ConfigureCryptoStep(provisioner: Provisioner) : AbstractStep(provisioner) {

    val hmac = aem.obj.typed<Any>()

    val hmacFile by lazy { provisioner.fileResolver.get(hmac.get()).file }

    val master = aem.obj.typed<Any>()

    val masterFile by lazy { provisioner.fileResolver.get(master.get()).file }

    override fun init() {
        logger.debug(listOf(
                "Resolved Crypto support files are located at paths:",
                "HMAC: $hmacFile",
                "Master: $masterFile"
        ).joinToString("\n"))
    }

    override fun action(instance: Instance) = instance.sync {
        instance.local {
            logger.info("Configuring Crypto Support using HMAC '$hmac' and master '$master' for $instance")
            val bundle = osgi.getBundle("com.adobe.granite.crypto.file")
            hmacFile.copyTo(bundle.dir.resolve("hmac"), true)
            masterFile.copyTo(bundle.dir.resolve("master"), true)
            osgi.restartBundle(bundle)
            logger.info("Configuring Crypto Support finished for $instance")
        }
    }

    init {
        id.set("configureCrypto")
        description.convention("Configuring Crypto Support")
    }
}
