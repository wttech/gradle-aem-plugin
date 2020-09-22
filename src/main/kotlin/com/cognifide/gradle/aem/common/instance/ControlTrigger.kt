package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.common.build.Behaviors

class ControlTrigger(aem: AemExtension) {

    val repeatAfter = aem.obj.long {
        convention(60_000L)
        aem.prop.long("localInstance.controlTrigger.repeatAfter")?.let { set(it) }
    }

    val repeatTimes = aem.obj.int {
        convention(2)
        aem.prop.int("localInstance.controlTrigger.repeatTimes")?.let { set(it) }
    }

    val poolInterval = aem.obj.long {
        convention(1000L)
        aem.prop.long("localInstance.controlTrigger.poolInterval")?.let { set(it) }
    }

    @Suppress("LoopWithTooManyJumpStatements")
    fun trigger(action: () -> Unit, verify: () -> Boolean, fail: () -> Unit) {
        var time = 0L
        var no = 0
        while (true) {
            if (time <= 0L || (System.currentTimeMillis() - time) >= repeatAfter.get()) {
                action()
                time = System.currentTimeMillis()
                no++
            }
            Behaviors.waitFor(poolInterval.get())
            if (verify()) {
                break
            }
            if (no == repeatTimes.get()) {
                fail()
                break
            }
        }
    }
}
