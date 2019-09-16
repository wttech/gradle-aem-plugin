package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.LocalInstance

abstract class LocalInstanceAction(aem: AemExtension) : AbstractAction(aem) {

    var instances: List<LocalInstance> = aem.localInstances
}
