package com.cognifide.gradle.aem.common.instance.tail

import java.time.ZoneId

class NoLogInfo : LogInfo {

    override val name: String = "unspecified"

    override val zoneId: ZoneId = ZoneId.systemDefault()
}
