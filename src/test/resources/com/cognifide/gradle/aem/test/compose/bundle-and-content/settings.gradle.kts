// Plugins configuration

pluginManagement {
	repositories {
		mavenLocal()
		jcenter()
	}
	resolutionStrategy {
		eachPlugin {
			when {
				requested.id.namespace == "com.cognifide.aem" -> useModule("com.cognifide.gradle:aem-plugin:6.0.0")
			}
		}
	}
}

// Project structure

rootProject.name = "example"