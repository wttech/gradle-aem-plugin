package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.provision.Provisioner
import com.cognifide.gradle.common.utils.toLowerCamelCase
import org.apache.commons.io.FilenameUtils

class DeployPackageStep(provisioner: Provisioner, val name: String, val url: Any) : AbstractStep(provisioner, "deployPackage/$name") {

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
        private val URL_DEPENDENCY_NOTATION = "[^:]+:([^:]+):[^:]+".toRegex()

        private val URL_EXTENSIONS = listOf(".zip", ".jar")

        private val URL_VERSION_PATTERNS = listOf(
                "[^.]+-(\\d+.\\d+.\\d+-\\w+)",
                "[^.]+-(\\d+.\\d+.\\w+)",
                "[^.]+-(\\d+.\\d+)",
                ".*-(\\d+.\\d+).*"
        ).map { it.toRegex() }

        fun deriveName(url: String): String? = when {
            URL_EXTENSIONS.any { url.endsWith(it) } -> url
                    .let { FilenameUtils.getBaseName(url) }
                    ?.let { baseName ->
                        URL_VERSION_PATTERNS.asSequence()
                                .mapNotNull { it.matchEntire(baseName)?.groupValues?.get(1) }
                                .firstOrNull()
                                ?.let { baseName.substringBefore("-$it") }
                    }
            else -> URL_DEPENDENCY_NOTATION.matchEntire(url)?.groupValues?.get(1)
        }?.replace(".", "_")?.toLowerCamelCase()
    }
}
