package com.cognifide.gradle.aem.common.instance.service.workflow

enum class Workflow(val workflowName: String) {
    DAM_CREATE_ASSET("update_asset_create_without_DM"),
    DAM_UPDATE_ASSET("update_asset_mod_without_DM");

    companion object {
        fun fromName(workflowName: String): Workflow? = values().find { it.workflowName == workflowName }
    }
}