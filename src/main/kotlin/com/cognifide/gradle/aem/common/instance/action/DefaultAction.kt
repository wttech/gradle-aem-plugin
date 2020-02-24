package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.InstanceAction

abstract class DefaultAction(protected val aem: AemExtension) : InstanceAction {

    protected val common = aem.common

    protected val logger = aem.logger
}
