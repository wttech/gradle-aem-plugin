package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance

abstract class AnyInstanceAction(aem: AemExtension) : AbstractAction(aem) {

    var instances: List<Instance> = aem.instances
}
