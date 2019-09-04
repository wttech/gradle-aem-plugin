package com.cognifide.gradle.aem.common.instance.service.workflow

enum class WorkflowType(val ids: List<String>) {
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
        fun of(type: String) = values().find { it.name.equals(type, ignoreCase = true) }

        fun ids(type: String) = ids(listOf(type))

        fun ids(types: Iterable<String>) = types.flatMap { of(it)?.ids ?: listOf(it) }
    }
}
