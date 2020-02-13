[![Cognifide logo](cognifide-logo.png)](http://cognifide.com)

<p>
  <img src="logo.png" alt="Gradle AEM Plugin"/>
</p>

# Bundle plugin

Should be applied to all projects that are composing CRX packages from both *OSGi bundle* being built and optionally *JCR content*. 

Inherits from [Package Plugin](#package-plugin).

## Bundle conventions

OSGi bundle jar base name and CRX package base name is computed from:

* for subproject of multi project build - `${project.rootProject.name}.${project.name}`,
* for single project build - `${project.name}` (just root project name).

Value of bundle `javaPackage` is computed from `${project.group}.${project.name}`.

*settings.gradle.kts*
```kotlin
includeProject(":aem:app.core")
```

*aem/build.gradle.kts*
```kotlin
allprojects {
    group = "com.company.example.aem"
}
```

Then below section is absolutely redundant:

*aem/sites/build.gradle.kts*
```kotlin
aem {
    tasks {
        bundleCompose {
            javaPackage = "${project.group}.${project.name}" // "com.company.example.aem.sites"
        }
    }
}
```

## Embedding JAR file into OSGi bundle

Use one of dedicated methods `bundleExportEmbed` or `bundlePrivateEmbed`.
These methods are above `bundleCompose` section as of dependency management is task agnostic and these methods 
are configuring `compileOnly` dependency and setting correct OSGi manifest entry at once (simplification).

```kotlin
aem {
    tasks {
        bundleExportEmbed("group:name:version", "com.group.name")
        // or 
        bundlePrivateEmbed("group:name:version", "com.group.name")
    }
}
```
 
For the reference, see [usage in AEM Multi-Project Example](https://github.com/Cognifide/gradle-aem-multi/blob/master/aem/common/build.gradle.kts).

## Configuring OSGi bundle manifest attributes

Plugin by default covers generation of few attributes by convention:

* `Bundle-Name` will grab value from `project.description`
* `Bundle-SymbolicName` will grab value from `javaPackage` (from section `aem.tasks.bundle`)
* `Bundle-Activator` will grab value from `javaPackage.activator` assuming that activator is an existing file named *Activator* or *BundleActivator* under *main* source set.
* `Sling-Model-Packages` will grab value from `javaPackage`
* `Export-Package` will grab value from `javaPackage`.

This values population behavior could be optionally disabled by bundle parameter `attributesConvention = false`.
Regardless if this behavior is enabled or disabled, all of values are overiddable e.g:

```kotlin
aem {
    tasks {
        bundleCompose {
            displayName = 'My Bundle"
            symbolicName = "com.company.aem.example.common"
            slingModelPackages = "com.company.aem.example.common.models"
            exportPackage("com.company.aem.example.common")
        }
    }
}
```

## Excluding packages being incidentally imported by OSGi bundle

Sometimes BND tool could generate *Import-Package* directive that will import too many OSGi classes to be available on bundle class path. Especially when we are migrating non-OSGi dependency to OSGi bundle (because of transitive class dependencies).
 
To prevent that we could generate own manifest entry that will prevent importing optional classes.

For instance: 

```kotlin
aem {
    tasks {
        bundleCompose {
            excludePackage("org.junit", "org.mockito")
            importPackage("!org.junit", "!org.mockito", "*") // alternatively
        } 
    }
}
```

## Task `bundleInstall`

Installs OSGi bundle on AEM instance(s).

Available options:

* `-Pbundle.deploy.awaited=false` - disable stability & health checks after deploying OSGi bundle.
* `-Pbundle.deploy.retry=n` - customize number of retries being performed after failed OSGi bundle installation.

## Task `bundleUninstall`

Uninstalls OSGi bundle on AEM instance(s).
