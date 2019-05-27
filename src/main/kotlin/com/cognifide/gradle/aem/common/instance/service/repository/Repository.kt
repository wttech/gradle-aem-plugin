package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync

class Repository(sync: InstanceSync) : InstanceService(sync) {

    internal val http = RepositoryHttpClient(aem, instance)

    var typeHints: Boolean = true

    var nullDeletes: Boolean = true

    var verbose: Boolean
        get() = http.responseChecks
        set(value) {
            http.responseChecks = value
        }

    fun node(path: String): Node {
        return Node(this, path)
    }
}
