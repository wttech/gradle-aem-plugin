package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.environment.io.ConfigExpander
import org.gradle.api.tasks.Internal

open class DockerTask : AemDefaultTask() {

    @Internal
    protected val stack = Stack()

    @Internal
    protected val config = ConfigExpander(aem)

    @Internal
    protected val options = aem.environmentOptions
}