package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import org.gradle.language.base.plugins.LifecycleBasePlugin
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

    @Input
    override val config = AemConfig.extendFromGlobal(project)

    init {
        description = "Composes AEM package from JCR content and built JAR bundles."
        group = AemPlugin.TASK_GROUP

        duplicatesStrategy = DuplicatesStrategy.WARN
        project.afterEvaluate({ assemble(project) })
    }

    @TaskAction
    override fun copy() {
        compose()
        super.copy()
    }

    protected open fun compose() {
        copyBundles(collectBundles())
    }

    fun assemble(project: Project, taskDeps: List<String> = listOf()) {
        includeContent(project)

        bundleCollectors += { collectBundles(project) }

        var deps = taskDeps
        if (deps.isEmpty()) {
            deps = if (project == this.project) listOf() else listOf(LifecycleBasePlugin.BUILD_TASK_NAME)
        }
        deps.forEach { taskName -> dependsOn("${project.path}:${taskName}") }
    }

    fun determineContentPath(project: Project): String {
        return project.projectDir.path + "/" + config.contentPath
    }

    open fun includeContent(project: Project) {
        val contentDir = File(determineContentPath(project))
        if (!contentDir.exists()) {
            logger.info("Package JCR root directory does not exist: ${contentDir.absolutePath}")
        }

        from(contentDir, {
            exclude(config.fileIgnores)
        })
    }

    open fun includeProfile(profileName: String) {
        includeVault(project.relativePath(config.vaultCommonPath))
        includeVault(project.relativePath(config.vaultProfilePath + "/" + profileName))
    }

    open fun includeVault(vltPath: Any) {
        into(AemPlugin.VLT_PATH) {
            from(vltPath)
        }
    }

    protected open fun collectBundles(project: Project) = JarCollector(project).all.toList()

    protected open fun collectBundles(): List<File> {
        return bundleCollectors.fold(TreeSet<File>(), { files, it -> files.addAll(it()); files }).toList()
    }

    protected fun copyBundles(jars: Collection<File>) {
        if (!jars.isEmpty()) {
            logger.info("Copying bundles into AEM package: " + jars.toString())
            into(config.bundlePath) { spec -> spec.from(jars) }
        }
    }
}
