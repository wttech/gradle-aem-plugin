package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.environment.ServiceAwait
import com.cognifide.gradle.aem.environment.docker.Stack
import org.gradle.api.tasks.Internal

open class DockerTask : AemDefaultTask() {

    @Internal
    protected val stack = Stack(aem, ServiceAwait(aem))
}