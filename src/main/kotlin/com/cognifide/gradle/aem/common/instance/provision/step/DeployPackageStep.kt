package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.provision.InstanceStep
import com.cognifide.gradle.aem.common.instance.provision.ProvisionException
import com.cognifide.gradle.aem.common.instance.provision.Provisioner
import com.cognifide.gradle.common.utils.toLowerCamelCase
import org.apache.commons.io.FilenameUtils
import org.gradle.api.artifacts.Dependency

class DeployPackageStep(provisioner: Provisioner) : AbstractStep(provisioner) {

    val source = aem.obj.typed<Any>()

    val name = aem.obj.string { convention(source.map { deriveName(it) }) }

    val pkg = source.map {
        val file = provisioner.fileResolver.get(it).file
        aem.packageOptions.wrapper.wrap(file)
    }

    override fun init() {
        logger.info("Resolved package '${name.get()}' to be deployed is located at path: '${pkg.get()}'")
    }

    override fun action(instance: Instance) = instance.sync {
        logger.info("Deploying package '${name.get()}' to $instance")
        awaitIf { packageManager.deploy(pkg.get()) }
    }

    fun isDeployedOn(instance: Instance) = instance.sync.packageManager.isDeployed(pkg.get())

    fun notDeployedOn(instance: Instance) = !isDeployedOn(instance)

    init {
        id.convention(name.map { "deployPackage/$it" })
        description.convention(name.map { "Deploying package '${name.get()}'" })
        version.convention(source.map { value ->
            when (value) {
                is String -> value
                is Dependency -> value.version
                else -> InstanceStep.VERSION_DEFAULT
            }
        })

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

        fun deriveName(source: Any): String = when (source) {
            is Dependency -> source.name
            is String -> tryDeriveName(source) ?: throw failDeriveName(source)
            else -> throw failDeriveName(source)
        }

        fun tryDeriveName(source: String): String? {
            val name = when {
                URL_EXTENSIONS.any { source.endsWith(it) } -> source
                        .let { FilenameUtils.getBaseName(source) }
                        ?.let { baseName ->
                            URL_VERSION_PATTERNS.asSequence()
                                    .mapNotNull { it.matchEntire(baseName)?.groupValues?.get(1) }
                                    .firstOrNull()
                                    ?.let { baseName.substringBefore("-$it") }
                        }
                else -> URL_DEPENDENCY_NOTATION.matchEntire(source)?.groupValues?.get(1)
            }
            return name?.replace(".", "_")?.toLowerCamelCase()
        }

        private fun failDeriveName(source: Any): ProvisionException {
            return ProvisionException("Package name cannot be derived from provided source '$source'! Please specify it explicitly.")
        }
    }
}
