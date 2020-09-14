package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.provision.Provisioner

class ConfigureCryptoStep(provisioner: Provisioner) : AbstractStep(provisioner) {

    val bundleSymbolicName = aem.obj.string {
        convention("com.adobe.granite.crypto.file")
        aem.prop.string("instance.provision.configureCrypto.bundleSymbolicName")?.let { set(it) }
    }

    val hmac = aem.obj.typed<Any>()

    val hmacFile by lazy { provisioner.fileResolver.get(hmac.get()).file }

    val master = aem.obj.typed<Any>()

    val masterFile by lazy { provisioner.fileResolver.get(master.get()).file }

    override fun init() {
        logger.debug(listOf(
                "Resolved Crypto files are located at paths:",
                "HMAC: $hmacFile",
                "Master: $masterFile"
        ).joinToString("\n"))
    }

    override fun action(instance: Instance) = instance.sync {
        instance.local {
            logger.info("Configuring Crypto started for $instance")

            val bundle = osgi.getBundle(bundleSymbolicName.get())
            val dataDir = bundle.dir.resolve("data")

            logger.info(listOf(
                    "Copying Crypto files to directory $dataDir:",
                    "HMAC: $hmacFile",
                    "Master: $masterFile"
            ).joinToString("\n"))
            hmacFile.copyTo(dataDir.resolve("hmac"), true)
            masterFile.copyTo(dataDir.resolve("master"), true)
            osgi.restartBundle(bundle)

            logger.info("Configuring Crypto finished for $instance")
        }
    }

    init {
        id.set("configureCrypto")
        description.convention("Configuring Crypto")
    }
}
