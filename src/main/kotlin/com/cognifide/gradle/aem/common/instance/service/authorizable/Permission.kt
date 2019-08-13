package com.cognifide.gradle.aem.common.instance.service.authorizable

enum class Permission(val property: String) {
    READ("jcr:read"),
    MODIFY("jcr:modifyProperties"),
    CREATE("jcr:addChildNodes"),
    DELETE("jcr:removeNode"),
    READ_ACL("jcr:readAccessControl"),
    EDIT_ACL("jcr:modifyAccessControl"),
    REPLICATE("crx:replicate");
}