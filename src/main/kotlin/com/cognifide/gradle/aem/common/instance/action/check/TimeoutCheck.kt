package com.cognifide.gradle.aem.common.instance.action.check

import com.cognifide.gradle.aem.common.instance.InstanceException

class TimeoutCheck(group: CheckGroup) : DefaultCheck(group) {

    var timeout = aem.props.long("instance.await.timeout") ?: 60000 // TODO corelate with instance

    override fun check() {
        if (base.action.running > timeout) {
            throw InstanceException("Instance timeout reached!")
        }
    }
}