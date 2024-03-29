package com.cognifide.gradle.aem.common.instance.service.repository

enum class ResourceType(val value: String) {
    ASSET("dam:Asset"),
    PAGE("cq:Page"),
    FILE("nt:file"),
    PAGE_CONTENT("cq:PageContent"),
    ASSET_CONTENT("dam:AssetContent"),
    WORKFLOW_MODEL("cq:WorkflowModel"),
    USER("rep:User");

    companion object {
        fun of(type: String) = values().find { it.value.equals(type, ignoreCase = true) }
    }
}
