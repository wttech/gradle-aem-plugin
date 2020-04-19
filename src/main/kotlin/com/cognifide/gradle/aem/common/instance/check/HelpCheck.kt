package com.cognifide.gradle.aem.common.instance.check

import org.osgi.framework.Bundle
import java.util.concurrent.TimeUnit

class HelpCheck(group: CheckGroup) : DefaultCheck(group) {

    /**
     * After longer inactivity time, try helping instance going back to healthy state.
     */
    val stateTime = aem.obj.long { convention(TimeUnit.MINUTES.toMillis(5)) }

    override fun check() {
        if (progress.stateTime >= stateTime.get()) {
            progress.stateData.getOrPut(STATE_HELPED) {
                help()
                true
            }
        }
    }

    // TODO start bundles in rounds, re-check after each round
    private fun help() = sync {
        val resolvedBundles = osgi.bundles.filter { !it.fragment && it.stateRaw == Bundle.RESOLVED }

        aem.common.progress(resolvedBundles.size) {
            aem.common.parallel.poolEach(resolvedBundles) { bundle ->
                increment("Starting bundle '${bundle.symbolicName}'") {
                    osgi.startBundle(bundle.symbolicName)
                }
            }
        }
    }

    companion object {
        private const val STATE_HELPED = "helped"
    }
}
