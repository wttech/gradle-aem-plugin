package com.cognifide.gradle.aem.common.instance.action.check

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.action.CheckAction

class TimeoutCheck(action: CheckAction, instance: Instance) : DefaultCheck(action, instance) {

    var timeout = aem.props.long("instance.await.timeout") ?: 60000 // TODO is global timeout acceptable?

    override fun check() {
        if (action.running > timeout) {
            throw InstanceException("Instance timeout reached!")
        }
    }
}