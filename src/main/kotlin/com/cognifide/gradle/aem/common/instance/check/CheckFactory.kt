package com.cognifide.gradle.aem.common.instance.check

class CheckFactory(val group: CheckGroup) {

    fun custom(callback: CustomCheck.() -> Unit) = CustomCheck(group, callback)

    fun bundles(options: BundlesCheck.() -> Unit = {}) = BundlesCheck(group).apply(options)

    fun installer(options: InstallerCheck.() -> Unit = {}) = InstallerCheck(group).apply(options)

    fun components(options: ComponentsCheck.() -> Unit = {}) = ComponentsCheck(group).apply(options)

    fun events(options: EventsCheck.() -> Unit = {}) = EventsCheck(group).apply(options)

    fun timeout(options: TimeoutCheck.() -> Unit = {}) = TimeoutCheck(group).apply(options)

    fun unavailable(options: UnavailableCheck.() -> Unit = {}) = UnavailableCheck(group).apply(options)

    fun unchanged(options: UnchangedCheck.() -> Unit = {}) = UnchangedCheck(group).apply(options)

    fun help(options: HelpCheck.() -> Unit = {}) = HelpCheck(group).apply(options)
}
