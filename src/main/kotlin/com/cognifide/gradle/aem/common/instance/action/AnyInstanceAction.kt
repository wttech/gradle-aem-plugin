package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance

abstract class AnyInstanceAction(aem: AemExtension) : AbstractAction(aem) {

    val instances = aem.obj.list<Instance> { convention(aem.obj.provider { aem.instances }) }
}
