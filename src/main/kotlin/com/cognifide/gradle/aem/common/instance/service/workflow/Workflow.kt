package com.cognifide.gradle.aem.common.instance.service.workflow

enum class Workflow(val ids: List<String>) {
    DAM_ASSET(listOf(
            "update_asset_create",
            "update_asset_create_without_DM",
            "update_asset_mod",
            "update_asset_mod_reupload",
            "update_asset_mod_without_DM",
            "update_asset_mod_without_DM_reupload",
            "dam_xmp_writeback"
    ));

    companion object {
        fun fromName(workflowName: String): Workflow? = values().find { it.ids.contains(workflowName) }
    }
}