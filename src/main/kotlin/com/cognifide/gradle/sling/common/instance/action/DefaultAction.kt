package com.cognifide.gradle.sling.common.instance.action

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.instance.InstanceAction

abstract class DefaultAction(protected val sling: SlingExtension) : InstanceAction {

    protected val common = sling.common

    protected val logger = sling.logger
}
