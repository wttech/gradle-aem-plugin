[![Cognifide logo](cognifide-logo.png)](http://cognifide.com)

<p>
  <img src="logo.png" alt="Gradle AEM Plugin"/>
</p>

# Bundle plugin

  * [About](#about)
  * [Bundle conventions](#bundle-conventions)
  * [Embedding JAR file into OSGi bundle](#embedding-jar-file-into-osgi-bundle)
  * [Configuring OSGi bundle manifest attributes](#configuring-osgi-bundle-manifest-attributes)
  * [Excluding packages being incidentally imported by OSGi bundle](#excluding-packages-being-incidentally-imported-by-osgi-bundle)
  * [Publishing bundles](#publishing-bundles)
  * [Task bundleInstall](#task-bundleinstall)
  * [Task bundleUninstall](#task-bundleuninstall)
  * [Known issues](#known-issues)
     * [BND tool error - Classes found in wrong directory](#bnd-tool-error---classes-found-in-wrong-directory)
     * [No OSGi services / components registered](#no-osgi-services--components-registered)

## About

Should be applied to all projects that are composing CRX packages from both *OSGi bundle* being built and optionally *JCR content*. 

To apply plugin use snippet:

```kotlin
plugins {
    id("com.cognifide.aem.bundle")
}
```

This plugin implicitly applies [Common Plugin](common-plugin.md).

When applying this plugin together with [Package Plugin](package-plugin.md), built OSGi bundle will be automatically put inside built CRX package.

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
import com.cognifide.gradle.aem.bundle.tasks.bundle

tasks {
    jar {
        bundle {
            javaPackage.set("${project.group}.${project.name}") // "com.company.example.aem.sites"
        }   
    }
}
```

## Embedding JAR file into OSGi bundle

Simply use dedicated method `embedPackage`.

That method is available for `jar` task in `bundle` task convention plugin section.

```kotlin
import com.cognifide.gradle.aem.bundle.tasks.bundle

tasks {
    jar {
        bundle {
            embedPackage("group:name:version", "com.group.name.*") // package will be a part of 'Private-Package'
            embedPackage("group:name:version", "com.group.name.*", export = true) // for 'Export-Package'
        }
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
import com.cognifide.gradle.aem.bundle.tasks.bundle

tasks {
    jar {
        bundle {
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
import com.cognifide.gradle.aem.bundle.tasks.bundle

tasks {
    jar {
        bundle {
            excludePackage("org.junit", "org.mockito")
        } 
    }
}
```

### Publishing bundles

Simply add following snippets to file _build.gradle.kts_ for each project applying bundle plugin.

```kotlin
plugins {
    id("com.cognifide.aem.bundle")
    id("maven-publish")
}

publishing {
    repositories {
        maven {
            // specify here e.g Nexus URL and credentials
        }   
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["aem"])
        }
    }
}
```

To publish bundle to repository (upload it to e.g Nexus repository) simply run one of [publish tasks](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:tasks) (typically `publish`).

It might be worth to configure [publishing repositories](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:repositories) globally. Consider moving `publishing { repositories { /* ... */ } }` section to root project's _build.gradle.kts_ into `allprojects { }` section. 
Then defining publishing repositories in each subproject will be no longer necessary.

## Task `bundleInstall`

Installs OSGi bundle on AEM instance(s).

Available options:

* `-Pbundle.deploy.awaited=false` - disable stability & health checks after deploying OSGi bundle.
* `-Pbundle.deploy.retry=n` - customize number of retries being performed after failed OSGi bundle installation.

## Task `bundleUninstall`

Uninstalls OSGi bundle on AEM instance(s).

## Known issues

### BND tool error - Classes found in wrong directory

After correcting bad Java package case from camelCase to lowercase according to [Oracle recommendations](https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html), BND tool may report error:

```
> Task :aem:sites:jar FAILED
...
error  : Classes found in the wrong directory: 
```

To fix above error simply run once following command to ensure building using fresh daemon and resources:

```bash
sh gradle clean :aem:sites:jar --no-daemon --rerun-tasks
```

### No OSGi services / components registered

Since AEM 6.2 it is recommended to use new OSGi service component annotations to register OSGi components instead SCR annotations (still supported, but not by Gradle AEM Plugin).

For the reference, please read post on official [Adobe Blog](http://blogs.adobe.com/experiencedelivers/experience-management/using-osgi-annotations-aem6-2/).

Basically, Gradle AEM Plugin is designed to be used while implementing new projects on AEM in version greater than 6.2.
Because, of that fact, there is no direct possibility to reuse code written for older AEM's which is using SCR annotations.
However it is very easy to migrate these annotations to new ones and generally speaking it is not much expensive task to do.

```java
import org.apache.felix.scr.annotations.Component;
```

->

```java
import org.osgi.service.component.annotations.Component;
```

New API fully covers functionality of old one, so nothing to worry about while migrating.
