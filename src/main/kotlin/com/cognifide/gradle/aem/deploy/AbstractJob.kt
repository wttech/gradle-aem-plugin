package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemInstance

abstract class AbstractJob(protected val instance: AemInstance, protected val config: AemConfig) {

    protected val sync = DeploySynchronizer(instance, config)

}