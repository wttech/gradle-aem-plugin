package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.AemDefaultTask
import org.gradle.api.tasks.Internal

open class DockerTask : AemDefaultTask() {

    @Internal
    protected val stack = Stack(aem)

    @Internal
    protected val config = ConfigExpander(aem.project.projectDir.path)

    @Internal
    protected val options = aem.environmentOptions.docker
}