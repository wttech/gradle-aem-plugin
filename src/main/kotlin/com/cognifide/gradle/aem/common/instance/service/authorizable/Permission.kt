package com.cognifide.gradle.aem.common.instance.service.authorizable

enum class Permission(val property: String) {
    READ("read"),
    MODIFY("modify"),
    CREATE("create"),
    DELETE("delete"),
    READ_ACL("acl_read"),
    EDIT_ACL("acl_edit"),
    REPLICATE("replicate");
}