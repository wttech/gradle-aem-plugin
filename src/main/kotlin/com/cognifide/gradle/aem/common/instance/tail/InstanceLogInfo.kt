package com.cognifide.gradle.aem.common.instance.tail

import com.cognifide.gradle.aem.common.instance.Instance
import java.time.ZoneId

class InstanceLogInfo(private val instance: Instance) : LogInfo {

    override val name: String get() = instance.name

    override val zoneId: ZoneId get() = instance.zoneId
}
