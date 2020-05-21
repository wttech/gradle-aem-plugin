package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.provision.step.DeployPackageStep.Companion.deriveName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DeployPackageStepTest {

    @Test
    fun shouldDeriveNameProperly() {
        // Real use cases
        assertEquals("searchWebconsolePlugin", deriveName("https://github.com/neva-dev/felix-search-webconsole-plugin" +
                "/releases/download/search-webconsole-plugin-1.3.0/search-webconsole-plugin-1.3.0.jar"))
        assertEquals("coreWcmComponentsAll", deriveName("com.adobe.cq:core.wcm.components.all:2.8.0@zip"))
        assertEquals("searchWebconsolePlugin", deriveName("com.neva.felix:search-webconsole-plugin:1.3.0"))

        // Fake / potential edge cases
        assertNull(deriveName("https://test.com/packages/neva-dev/felix-search-webconsole-1.0"))
        assertEquals("felixSearchWebconsole", deriveName("https://test.com/some-dir/felix-search-webconsole-1.0.zip"))
        assertEquals("felixSearchWebconsole", deriveName("https://test.com/some-dir/felix-search-webconsole-1.0.jar"))
        assertEquals("felixSearchWebconsole", deriveName("https://test.com/aa/bb/cc/felix.search.webconsole-1.0.ALPHA.zip"))
        assertEquals("felixSearchWebconsole", deriveName("https://test.com/some-dir/felix-search-webconsole-1.1.23.zip"))
        assertEquals("felixSearchWebconsole", deriveName("https://test.com/felix-search-webconsole-1.0.0-SNAPSHOT.zip"))
    }
}
