package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.ProgressIndicator
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip as Base

open class ZipTask : Base(), AemTask {

    @Nested
    final override val aem = AemExtension.of(project)

    @Internal
    var copyProgress: ProgressIndicator.() -> Unit = {
        update("Creating ZIP file: ${archiveFileName.get()}, current size: ${Formats.size(archiveFile.get().asFile)}")
    }

    init {
        group = AemTask.GROUP
        isZip64 = true
    }

    @TaskAction
    override fun copy() {
        aem.progressIndicator {
            updater = copyProgress
            super.copy()
            logger.info("ZIP file created: ${archiveFile.get().asFile}")
        }
    }
}