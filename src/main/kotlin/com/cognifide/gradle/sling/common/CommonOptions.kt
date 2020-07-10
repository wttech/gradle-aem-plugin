package com.cognifide.gradle.sling.common

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.utils.LineSeparator
import org.gradle.internal.os.OperatingSystem

open class CommonOptions(private val sling: SlingExtension) {

    private val project = sling.project

    /**
     * Base name used as default for CRX packages being created by compose or collect task
     * and also for OSGi bundle JARs.
     */
    val baseName = sling.obj.string {
        convention(sling.obj.provider {
            (if (project == project.rootProject) {
                project.rootProject.name
            } else {
                "${project.rootProject.name}-${project.name}"
            })
        })
    }

    /**
     * Allows to disable features that are using running Sling instances.
     *
     * It is more soft offline mode than Gradle's one which does much more.
     * It will not use any Maven repository so that CI build will fail which is not expected in e.g integration tests.
     */
    val offline = sling.obj.boolean { convention(sling.prop.flag("offline")) }

    /**
     * Determines current environment name to be used in e.g package deployment.
     */
    val env = sling.obj.string {
        convention(System.getenv("ENV") ?: "local")
        sling.prop.string("env")?.let { set(it) }
    }

    /**
     * Specify characters to be used as line endings when cleaning up checked out JCR content.
     */
    val lineSeparator = sling.obj.typed<LineSeparator> {
        convention(LineSeparator.LF)
        sling.prop.string("lineSeparator")?.let { set(LineSeparator.of(it)) }
    }

    val archiveExtension = sling.obj.string {
        convention(sling.project.provider { if (OperatingSystem.current().isUnix) "tar.gz" else "zip" })
        sling.prop.string("archiveExtension")?.let { set(it) }
    }

    val executableExtension = sling.obj.string {
        convention(sling.project.provider { if (OperatingSystem.current().isWindows) ".bat" else "" })
        sling.prop.string("executableExtension")?.let { set(it) }
    }
}
