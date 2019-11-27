package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.common.instance.Instance
import java.time.ZoneId

data class InstanceLoggingInfo(val name: String, val zoneId: ZoneId) {

    companion object {
        fun default() = InstanceLoggingInfo("unspecified", ZoneId.systemDefault())
        fun of(instance: Instance) = InstanceLoggingInfo(instance.name, instance.zoneId)
    }
}
