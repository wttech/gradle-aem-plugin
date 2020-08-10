package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.service.osgi.Bundle
import com.cognifide.gradle.common.CommonException
import java.util.concurrent.TimeUnit

class HelpCheck(group: CheckGroup) : DefaultCheck(group) {

    /**
     * After longer inactivity time, try helping instance going back to healthy state.
     */
    val stateTime = aem.obj.long { convention(TimeUnit.MINUTES.toMillis(8)) }

    /**
     * Bundle with these states are considered for forcing start.
     */
    val bundleStartStates = aem.obj.strings {
        convention(listOf(
                Bundle.STATE_RESOLVED,
                Bundle.STATE_INSTALLED
        ))
    }

    /**
     * Repeat bundle starting few times (brute-forcing).
     */
    var bundleStartRetry = common.retry { afterSquaredSecond(3) }

    /**
     * Time to wait after starting bundles.
     */
    val bundleStartDelay = aem.obj.long { convention(TimeUnit.SECONDS.toMillis(3)) }

    override fun check() {
        if (progress.stateTime >= stateTime.get()) {
            progress.stateData.getOrPut(STATE_HELPED) {
                help()
                true
            }
        }
    }

    private fun help() = sync {
        startBundles()
    }

    private fun InstanceSync.startBundles() {
        logger.info("Trying to start OSGi bundles on $instance")
        var startable = listOf<Bundle>()

        try {
            bundleStartRetry.withCountdown<Unit, CommonException>("start bundles on '${instance.name}'") {
                startable = startableBundles()
                if (startable.isNotEmpty()) {
                    startBundles(startable)
                    startable = startableBundles()
                    if (startable.isNotEmpty()) {
                        throw HelpException("Starting bundles to be repeated on $instance!")
                    }
                }
            }
        } catch (e: HelpException) {
            logger.warn("Bundles (${startable.size}) cannot be started automatically on $instance:\n" +
                    startable.joinToString("\n"))
        }
    }

    private fun ignoredBundles() = group.checks.filterIsInstance<BundlesCheck>()
            .firstOrNull()?.symbolicNamesIgnored?.get()
            ?: listOf<String>()

    private fun InstanceSync.startableBundles(): List<Bundle> = osgi.bundles.asSequence()
            .filter { !ignoredBundles().contains(it.symbolicName) }
            .filter { !it.fragment && bundleStartStates.get().contains(it.state) }
            .toList()

    private fun InstanceSync.startBundles(bundles: List<Bundle>) {
        common.parallel.poolEach(bundles) { startBundle(it) }
        Thread.sleep(bundleStartDelay.get())
    }

    private fun InstanceSync.startBundle(bundle: Bundle) = try {
        osgi.startBundle(bundle.symbolicName)
    } catch (e: CommonException) {
        logger.debug("Cannot start bundle on $instance!", e)
    }

    companion object {
        private const val STATE_HELPED = "helped"
    }
}
