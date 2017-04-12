package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input

open class AssembleTask : PackageTask() {

    companion object {
        val NAME = "aemAssemble"
    }

    @Input
    var assemblyType = "full"

    init {
        description = "Assembles AEM package"
    }

    override fun compose() {
        super.compose()
        includeProfile(assemblyType)
    }

    override fun includeContent(project: Project) {
        from(determineContentPath(project), {
            exclude(effectiveFileIgnores)
            exclude(AemPlugin.VLT_PATH)
        })
    }

    override val effectiveFileExpandProperties: Map<String, Any?>
        get() = super.effectiveFileExpandProperties + mapOf("packageType" to assemblyType)

}
