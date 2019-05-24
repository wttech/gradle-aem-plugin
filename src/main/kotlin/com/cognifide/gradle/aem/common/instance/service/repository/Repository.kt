package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync

class Repository(sync: InstanceSync) : InstanceService(sync) {

    val typeHints: Boolean = true

    val nullDeletes: Boolean = true

    fun node(path: String): Node {
        return Node(this, path)
    }
}
