package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.provision.ProvisionException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DeployPackageSourceTest {

    @Test
    fun shouldDerivePropertiesProperly() {
        // Real use cases
        assertEquals(
            DeployPackageSource("search-webconsole-plugin", "1.3.0"),
            DeployPackageSource.fromUrlOrNotation(
                "https://github.com/neva-dev/felix-search-webconsole-plugin" +
                    "/releases/download/search-webconsole-plugin-1.3.0/search-webconsole-plugin-1.3.0.jar"
            )
        )
        assertEquals(
            DeployPackageSource("core.wcm.components.all", "2.8.0"),
            DeployPackageSource.fromUrlOrNotation("com.adobe.cq:core.wcm.components.all:2.8.0@zip")
        )
        assertEquals(
            DeployPackageSource("search-webconsole-plugin", "1.3.0"),
            DeployPackageSource.fromUrlOrNotation("com.neva.felix:search-webconsole-plugin:1.3.0")
        )

        // Fake / potential edge cases
        assertThrows<ProvisionException> {
            DeployPackageSource.fromUrlOrNotation("https://test.com/packages/neva-dev/felix-search-webconsole-1.0")
        }

        assertEquals(
            DeployPackageSource("felix-search-webconsole", "1.0"),
            DeployPackageSource.fromUrlOrNotation("https://test.com/some-dir/felix-search-webconsole-1.0.zip")
        )
        assertEquals(
            DeployPackageSource("felix-search-webconsole", "1.0"),
            DeployPackageSource.fromUrlOrNotation("https://test.com/some-dir/felix-search-webconsole-1.0.jar")
        )
        assertEquals(
            DeployPackageSource("felix.search.webconsole", "1.0.ALPHA"),
            DeployPackageSource.fromUrlOrNotation("https://test.com/aa/bb/cc/felix.search.webconsole-1.0.ALPHA.zip")
        )
        assertEquals(
            DeployPackageSource("felix-search-webconsole", "1.1.23"),
            DeployPackageSource.fromUrlOrNotation("https://test.com/some-dir/felix-search-webconsole-1.1.23.zip")
        )
        assertEquals(
            DeployPackageSource("felix-search-webconsole", "1.0.0.RELEASE"),
            DeployPackageSource.fromUrlOrNotation("https://test.com/some-dir/felix-search-webconsole-1.0.0.RELEASE.zip")
        )
        assertEquals(
            DeployPackageSource("felix-search-webconsole", "1.0.0-SNAPSHOT"),
            DeployPackageSource.fromUrlOrNotation("https://test.com/felix-search-webconsole-1.0.0-SNAPSHOT.zip")
        )
    }
}
