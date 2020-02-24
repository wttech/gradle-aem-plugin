package com.cognifide.gradle.aem.common.instance.tail

import com.cognifide.gradle.aem.common.instance.Instance
import java.time.ZoneId

data class InstanceLogInfo(val name: String, val zoneId: ZoneId) {

    companion object {
        fun none() = InstanceLogInfo("unspecified", ZoneId.systemDefault())
        fun of(instance: Instance) = InstanceLogInfo(instance.name, instance.zoneId)
    }
}
