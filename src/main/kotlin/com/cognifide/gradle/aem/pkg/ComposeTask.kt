package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.bundling.Zip
import java.io.File
import java.util.*

/**
 * TODO Input is also effected by SCR plugin / metadata?
 */
open class ComposeTask : Zip(), AemTask {

    companion object {
        val NAME = "aemCompose"
    }

    @Internal
    var bundleCollectors: List<() -> List<File>> = mutableListOf()

    @Internal
    var contentCollectors: List<() -> Unit> = mutableListOf()

    @Input
    override val config = AemConfig.extendFromGlobal(project)

    init {
        description = "Composes AEM / CRX package from JCR content and built JAR bundles."
        group = AemPlugin.TASK_GROUP

        duplicatesStrategy = DuplicatesStrategy.WARN
        project.afterEvaluate({ includeProject(project) })

        project.gradle.projectsEvaluated({
            fromContents()
            fromBundles()
        })
    }

    private fun determineContentPath(project: Project): String {
        val task = project.tasks.getByName(ComposeTask.NAME) as ComposeTask

        return project.projectDir.path + "/" + task.config.contentPath
    }

    private fun fromBundles() {
        val jars = bundleCollectors.fold(TreeSet<File>(), { files, it -> files.addAll(it()); files }).toList()
        if (jars.isEmpty()) {
            logger.info("No bundles to copy into AEM package")
        } else {
            logger.info("Copying bundles into AEM package: " + jars.toString())
            into(config.bundlePath) { spec -> spec.from(jars) }
        }
    }

    private fun fromContents() {
        contentCollectors.onEach { it() }
    }

    fun includeProject(projectPath: String) {
        includeProject(project.findProject(projectPath))
    }

    fun includeProject(project: Project) {
        includeContent(project)
        includeBundles(project)

        dependsOn("${project.path}:${BasePlugin.ASSEMBLE_TASK_NAME}")
    }

    fun includeBundles(projectPath: String) {
        includeBundles(project.findProject(projectPath))
    }

    fun includeBundles(project: Project) {
        bundleCollectors += {
            JarCollector(project).all.toList()
        }
    }

    fun includeContent(projectPath: String) {
        includeContent(project.findProject(projectPath))
    }

    fun includeContent(project: Project) {
        contentCollectors += {
            val contentDir = File(determineContentPath(project))
            if (!contentDir.exists()) {
                logger.info("Package JCR content directory does not exist: ${contentDir.absolutePath}")
            } else {
                logger.info("Copying JCR content from: ${contentDir.absolutePath}")

                from(contentDir, {
                    exclude(config.fileIgnores)
                })
            }
        }
    }

    fun includeVault(vltPath: Any) {
        into(AemPlugin.VLT_PATH) {
            from(vltPath)
        }
    }

    fun includeVaultProfile(profileName: String) {
        includeVault(project.relativePath(config.vaultCommonPath))
        includeVault(project.relativePath(config.vaultProfilePath + "/" + profileName))
    }
}
