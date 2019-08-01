package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.utils.Formats

object VersionUtil {

    private const val PATH_6_1 = "/etc/workflow/launcher/config/"

    private const val PATH_6_4 = "/conf/global/settings/workflow/launcher/config/"

    fun getLauncherPath(workflowName: String, aemVersion: String): String {
        return if (Formats.versionAtLeast(aemVersion, "6.4.0")) PATH_6_4 + workflowName
        else PATH_6_1 + workflowName
    }
}