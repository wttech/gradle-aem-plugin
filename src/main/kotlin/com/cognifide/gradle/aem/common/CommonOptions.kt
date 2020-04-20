package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.LineSeparator
import org.gradle.internal.os.OperatingSystem

open class CommonOptions(private val aem: AemExtension) {

    private val project = aem.project

    /**
     * Base name used as default for CRX packages being created by compose or collect task
     * and also for OSGi bundle JARs.
     */
    val baseName = aem.obj.string {
        convention(aem.obj.provider {
            (if (project == project.rootProject) {
                project.rootProject.name
            } else {
                "${project.rootProject.name}-${project.name}"
            })
        })
    }

    /**
     * Allows to disable features that are using running AEM instances.
     *
     * It is more soft offline mode than Gradle's one which does much more.
     * It will not use any Maven repository so that CI build will fail which is not expected in e.g integration tests.
     */
    val offline = aem.obj.boolean { convention(aem.prop.flag("offline")) }

    /**
     * Determines current environment name to be used in e.g package deployment.
     */
    val env = aem.obj.string {
        convention(System.getenv("ENV") ?: "local")
        aem.prop.string("env")?.let { set(it) }
    }

    /**
     * Specify characters to be used as line endings when cleaning up checked out JCR content.
     */
    val lineSeparator = aem.obj.typed<LineSeparator> {
        convention(LineSeparator.LF)
        aem.prop.string("lineSeparator")?.let { set(LineSeparator.of(it)) }
    }

    val archiveExtension = aem.obj.string {
        convention(aem.project.provider { if (OperatingSystem.current().isUnix) "tar.gz" else "zip" })
        aem.prop.string("archiveExtension")?.let { set(it) }
    }

    val executableExtension = aem.obj.string {
        convention(aem.project.provider { if (OperatingSystem.current().isWindows) ".bat" else "" })
        aem.prop.string("executableExtension")?.let { set(it) }
    }
}
