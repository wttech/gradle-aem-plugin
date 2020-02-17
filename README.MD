[![Cognifide logo](docs/cognifide-logo.png)](http://cognifide.com)

[![Gitter](https://badges.gitter.im/Cognifide/gradle-aem-plugin.svg)](https://gitter.im/Cognifide/gradle-aem-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Download](https://api.bintray.com/packages/cognifide/maven-public/gradle-aem-plugin/images/download.svg) ](https://bintray.com/cognifide/maven-public/gradle-aem-plugin/_latestVersion)
[![Build Status](https://dev.azure.com/gradle-aem/gradle-aem-plugin/_apis/build/status/Continuous%20Check?branchName=master)](https://dev.azure.com/gradle-aem/gradle-aem-plugin/_build/latest?definitionId=6&branchName=master)
[![Gradle Status](https://gradleupdate.appspot.com/Cognifide/gradle-aem-plugin/status.svg?random=123)](https://gradleupdate.appspot.com/Cognifide/gradle-aem-plugin/status)
[![Apache License, Version 2.0, January 2004](docs/apache-license-badge.svg)](http://www.apache.org/licenses/)

<p>
  <img src="docs/logo.png" alt="Gradle AEM Plugin"/>
</p>

## Table of contents

  * [About](#about)
     * [Screenshot](#screenshot)
     * [Features](#features)
     * [Important notice](#important-notice)
  * [Getting started](#getting-started)
  * [Plugins](#plugins)
     * [Plugins setup](#plugins-setup)
        * [Minimal plugins setup](#minimal-plugins-setup)
        * [Complete plugins setup](#complete-plugins-setup)
     * [Plugins documentation](#plugins-documentation)
  * [How to's](#how-tos)
     * [Set AEM configuration properly for all / concrete project(s)](#set-aem-configuration-properly-for-all--concrete-projects)
     * [Understand why there are one or two plugins to be applied in build script](#understand-why-there-are-one-or-two-plugins-to-be-applied-in-build-script)
     * [Work effectively on start and daily basis](#work-effectively-on-start-and-daily-basis)
     * [Customize convention for CRX package and OSGi bundle names and paths](#customize-convention-for-crx-package-and-osgi-bundle-names-and-paths)
  * [Compatibility](#compatibility)
  * [Building](#building)
  * [Testing](#testing)
  * [Contributing](#contributing)
  * [License](#license)

## About

Swiss army knife for AEM related automation. Incremental build which takes seconds, not minutes. Developer who does not loose focus between build time gaps. Extend freely your build system directly in project. 

AEM developer - it's time to meet Gradle! You liked or used plugin? Don't forget to **star this project** on GitHub :)

Be inspired by watching [live demo](https://adapt.to/2018/en/schedule/a-better-developer-experience-for-sling-based-applications.html) presented on official **Sling adaptTo() 2018** conference.

Looking for dedicated version of plugin for [**Apache Sling**](https://sling.apache.org)? Check out [Gradle Sling Plugin](https://github.com/Cognifide/gradle-sling-plugin)!

### Screenshot

<p align="center">
  <img src="docs/gradle-aem-multi-build.gif" alt="Gradle AEM Multi Build"/>
</p>

What is being done above by simply running super easy command `sh gradlew`?

* `:aem:instanceSatisfy` -> checking for new dependent CRX packages to be deployed (in a lazy & fail-safe manner) that could arrive to our AEM instances automatically if somebody else add it to build configuration in the meantime.
* `:aem:assembly:full:packageDeploy` -> building & deploying all-in-one CRX package to AEM instances in parallel, then awaiting for stable condition of AEM instances and built application.

**Build is incremental** which guarantees optimized time every time regardless of build command used.
Only changed parts of application are processed again:

* Dependent CRX packages are installed only when they are not already installed on particular AEM instances.
* CRX package is rebuild only when JCR content / files under *jcr_root* are changed.
* Java code is recompiled only when code in *\*.java* files is changed.
* Front-end / Webpack build is run again only when code in *\*.scss* and *\*.js* etc files is changed.

Want to see it in action? Follow [here](https://github.com/Cognifide/gradle-aem-multi)!

### Features 

* Automated [native AEM instances](#instance-plugin) setup optimized for best development experience.
* [Powerful AEM DSL scripting capabilities](#implementing-tasks) for performing JCR content migrations, managing AEM instances, integrating with Docker based tools.
* [Advanced AEM instance(s) stability & health checking](#task-instanceawait) after CRX package deployment.
* [Continuous AEM incident monitoring](#task-instancetail) and interactive reporting (centralized log tailing of any AEM instances with no SSH).
* Easy parallel [CRX package deployment](#task-packagedeploy) to many remote group of instances.
* [Fail-safe dependent CRX packages installation](#task-instancesatisfy) from local and remote sources using various protocols (SMB / SSH / HTTP / custom).
* [Fast JCR content synchronization](#task-packagesync) from running AEM instances with advanced content normalization.
* [Composing CRX package](#task-packagecompose) from multiple separate JCR content roots, bundles.
* [Validating CRX package](#crx-package-validation) using seamless integration with [OakPAL tool](http://adamcin.net/oakpal).
* [All-in-one CRX packages generation](#assembling-packages-merging-all-in-one) (assemblies), vault filters merging etc.
* [Easy OSGi bundle customization](#bundle-plugin) with BND tool embedded.

Gradle AEM Plugin is following strategy [convention over configuration](https://en.wikipedia.org/wiki/Convention_over_configuration). When following built-in conventions about project structure & naming, then only minimal configuration is required. 
Still all features are **fully configurable**.

### Important notice 

Major releases of plugin are introducing breaking changes. Build functionality is mostly covered, however build scripts need corrections.

Documentation for previous series:

* [6.2.0](https://github.com/Cognifide/gradle-aem-plugin/tree/6.2.0) (last in 6.x serie / with *aem* prefixes in task / property names)
* [5.1.4](https://github.com/Cognifide/gradle-aem-plugin/tree/5.1.4) (last supporting Groovy DSL)

## Getting started

Most effective way to experience Gradle AEM Plugin is to use *Quickstart* located in:
  * [AEM Single-Project Example](https://github.com/Cognifide/gradle-aem-single#quickstart) - recommended for **application/library** development,
  * [AEM Multi-Project Example](https://github.com/Cognifide/gradle-aem-multi#quickstart) - recommended for **long-term project** development,
  * [AEM Boot](https://github.com/Cognifide/gradle-aem-boot#quickstart) - only booting local AEM instances and AEM dispatcher automatically. Useful when building CRX packages is covered separately, e.g by Maven & [Content Package Maven Plugin](https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/vlt-mavenplugin.html).
  
The only software needed on your machine to start using plugin is Java 8 or newer (also to setup local native AEM instances).
Optionally, [Docker](https://www.docker.com/) is needed (when using automatic AEM dispatcher setup).

As a build command, it is recommended to use Gradle Wrapper (`gradlew`) instead of locally installed Gradle (`gradle`) to easily have same version of build tool installed on all environments. Only at first build time, wrapper will be automatically downloaded and installed, then reused.

## Plugins 

### Plugins setup

Released versions of plugin are available on: 

* [Gradle Plugin Portal](https://plugins.gradle.org/search?term=com.cognifide.aem),
* [Bintray](https://bintray.com/cognifide/maven-public/gradle-aem-plugin) (when using this repository proper URL need to be added into `buildscript` section).

Recommended way is to apply plugin using Gradle Plugin Portal and techniques described there.

#### Minimal plugins setup

Configuration below assumes building and deploying CRX packages to AEM instance(s) via command: `gradlew packageDeploy`.

File *build.gradle.kts*:

```kotlin
plugins {
    id("com.cognifide.aem.bundle") version "<version>" // or 'package' for JCR content only
}

group = "com.company.example.aem.core"
```

#### Complete plugins setup

Illustrative configuration below assumes building and deploying on AEM instance(s) via command: `gradlew` (default tasks will be used).
Intention of snippet below is to demonstrate: 

* How particular values could be customized via Gradle AEM DSL,
* What are the default values and how they are determined.

```kotlin
plugins {
    id("com.cognifide.aem.bundle") version "<version>"
    id("com.cognifide.aem.instance") version "<version>"
    id("org.jetbrains.kotlin.jvm") // or any other like 'java' to compile OSGi bundle
}

group = "com.company.aem"
version = "1.0.0"
defaultTasks(":instanceSatisfy", ":instanceProvision", ":packageDeploy")

aem {
    `package` { // built CRX package options
        contentDir = project.file("src/main/content")
        installPath = when {
            project == project.rootProject -> "/apps/${project.rootProject.name}/install"
            else -> "/apps/${project.rootProject.name}/${projectName}/install"
        }
        nodeTypesSync("PRESERVE_AUTO")
        validator {
            enabled = prop.boolean("package.validator.enabled") ?: true
            verbose = prop.boolean("package.validator.verbose") ?: true
            planName = prop.string("package.validator.plan") ?: "default-plan.json"
            severity("MAJOR")
        }       
        // ...
    }
    instance { // AEM instances to work with
        local("http://localhost:4502") // local-author
        local("http://localhost:4503") // local-publish
        remote("http://192.168.100.101:4502", "int-author")
        remote("http://192.168.100.101:4503", "int-publish")
        // etc
        
        http { // allows to customize HTTP connection to AEM instances
            connectionTimeout = prop.int("instance.http.connectionTimeout") ?: 30000
            connectionRetries = prop.boolean("instance.http.connectionRetries") ?: true
            connectionIgnoreSsl = prop.boolean("instance.http.connectionIgnoreSsl") ?: true
    
            proxyHost = prop.string("instance.http.proxyHost")
            proxyPort = prop.int("instance.http.proxyPort")
            proxyScheme = prop.string("instance.http.proxyScheme")
        }
    }
    localInstance { // config for AEM instances to be created on local file system
        quickstart {
            jarUrl = prop.string("localInstance.quickstart.jarUrl")
            licenseUrl = prop.string("localInstance.quickstart.licenseUrl")
        }
        backup {
            uploadUrl = prop.string("localInstance.backup.uploadUrl")
            downloadUrl = prop.string("localInstance.backup.downloadUrl")
        }
        install {
            files { // CRX packages and OSGi bundles to be pre-installed on created AEM instances
                download("http://.../package.zip") // CRX package downloaded over HTTP
                resolve("group:name:version") // OSGi bundle from Maven repository
            }
        }
        init { // hook called once in scope of instance just created and up first time
            logger.info("Initializing instance '$name'")
            sync {
                // ...
            }
        }   
        rootDir = prop.string("localInstance.root")
        // ...
    }

    tasks {
        bundleCompose { 
            // customizing OSGi bundle manifest
            description = "Example application built by GAP"
            docUrl = "https://github.com/Cognifide/gradle-aem-example"
            exportPackage("com.company.example.aem.*") 
            slingModelPackages = "com.company.example.aem"
    
            // for checking OSGi component health on runtime
            javaPackage = "com.company.example.aem" 
            // ...
        }
        packageCompose { // customizing built CRX package
            fromProject(":core")
            fromProject(":config")
            
            baseName = 'example-for-changing-zip-name'
            
            vaultDefinition { // place for overriding CRX Package / Vault properties, defining hooks
                // ...
            }
        }
        instanceProvision {
            step("enable-crxde") {
                description = "Enables CRX DE"
                condition { once() && instance.environment != "prod" }
                action {
                    sync {
                        osgiFramework.configure("org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet", mapOf(
                                "alias" to "/crx/server"
                        ))
                    }
                }
            }
            // ...
        }       
        instanceSatisfy { // customizing CRX packages to be deployed as dependencies before built AEM application
            packages {
                "my-package" { download("http://.../my-package-1.0.0.zip") }
                "my-bundle" { resolve("group:name:version") } // will be wrapped on-the-fly to CRX package
            }
        }
        // ... and all other tasks
    }
}
```

To see all available options and actual documentation, please follow to:

* `aem` - [AemExtension](src/main/kotlin/com/cognifide/gradle/aem/AemExtension.kt)
* `package` - [PackageOptions](src/main/kotlin/com/cognifide/gradle/aem/common/pkg/PackageOptions.kt)
* `instance` - [InstanceOptions](src/main/kotlin/com/cognifide/gradle/aem/common/instance/InstanceOptions.kt)
* `localInstance` - [LocalInstanceManager](src/main/kotlin/com/cognifide/gradle/aem/instance/LocalInstanceManager.kt)
* `fileTransfer` - [FileTransferManager](src/main/kotlin/com/cognifide/gradle/aem/common/file/transfer/FileTransferManager.kt)
* `bundleCompose` - [BundleCompose](src/main/kotlin/com/cognifide/gradle/aem/bundle/tasks/BundleCompose.kt)
* `packageCompose` - [PackageCompose](src/main/kotlin/com/cognifide/gradle/aem/pkg/tasks/PackageCompose.kt)
* `instanceProvision` - [InstanceProvision](src/main/kotlin/com/cognifide/gradle/aem/instance/provision/InstanceProvision.kt)
* `instanceSatisfy` - [InstanceSatisfy](src/main/kotlin/com/cognifide/gradle/aem/instance/satisfy/InstanceSatisfy.kt)
* `...` - other tasks in similar way.

### Plugins documentation

Gradle AEM Plugin to be more concise is now more like set of plugins.
Each plugin has its own documentation:

* [Common](docs/common-plugin.md)
* [Package](docs/package-plugin.md)
* [Package Sync](docs/package-sync-plugin.md)
* [Bundle](docs/bundle-plugin.md)
* [Instance](docs/instance-plugin.md)
* [Local Instance](docs/local-instance-plugin.md)

## How to's

### Set AEM configuration properly for all / concrete project(s)

Common configuration like root of content for JCR package, should be defined in `allprojects` section like below / e.g in root *build.gradle.kts* file:

```kotlin
allprojects {
  plugins.withId("com.cognifide.aem.common") {
    configure<AemExtension> {
        `package` {
            contentDir = project.file("src/main/aem") // overrides default dir named 'content'
        }
    }
  }
  
  plugins.withId("com.cognifide.aem.bundle") {
    configure<AemExtension> {
        tasks {
            bundleCompose {
                category = "example"
                vendor = "Company"
            }
        }
    }
  
    dependencies {
        "compileOnly"("com.adobe.aem:uber-jar:${Build.AEM_VERSION}:apis") // and more
    }
  }
}
```

For instance, subproject `:aem:core` specific configuration like OSGi bundle or CRX package options should be defined in `aem/core/build.gradle.kts`:

```kotlin
aem {
    tasks {
        bundleCompose {
            javaPackage = "com.company.example.aem.core"
        }
        packageCompose {
            fromProjects(':content:*')
            baseName = "example-core"
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}
```

Warning! Very often plugin users mistake is to configure `instanceSatisfy` task in `allprojects` closure. 
As an effect there will be same dependent CRX package defined multiple times.
 
### Understand why there are one or two plugins to be applied in build script

Gradle AEM Plugin assumes separation of 5 plugins to properly fit into Gradle tasks structure correctly.

Most often, Gradle commands are being launched from project root and tasks are being run by their name e.g `instanceSatisfy` (which is not fully qualified, better if it will be `:instanceSatisfy` of root project).
Let's imagine if task `instanceSatisfy` will come from package plugin, then Gradle will execute more than one `instanceSatisfy` (for all projects that have plugin applied), so that this is unintended behavior.
Currently used plugin architecture solves that problem.

### Work effectively on start and daily basis

Initially, to create fully configured local AEM instances simply run command `gradlew instanceSetup`.

Later during development process, building and deploying to AEM should be done using the simplest command: `gradlew`.
Above configuration uses [default tasks](https://docs.gradle.org/current/userguide/tutorial_using_tasks.html#sec:default_tasks), so that alternatively it is possible to do the same using explicitly specified command `gradlew instanceSatisfy packageDeploy`.

* Firstly dependent packages (like AEM hotfixes, Vanity URL Components etc) will be installed lazily (only when they are not installed yet).
* In next step application is being built and deployed to all configured AEM instances.
* Finally build awaits till all AEM instances are stable.

### Customize convention for CRX package and OSGi bundle names and paths

Because of [bug](https://github.com/gradle/gradle/issues/8401) related with regresion introduced in Gradle 5.1, there are some difficulties with setting archives base names.
AEM Plugin is overriding default Gradle convention for not only having project name in archive base name, but also to having prefix - root project name when project is one of subprojects (multi-project build case as in [Gradle AEM Multi](https://github.com/Cognifide/gradle-aem-multi/)).
However overriding this convention might not be trivial and is not recommended as of AEM Plugin in most cases proposes **good enough battle-tested convention**. 

Still, if it is really needed to be done - setting customized name for CRX packages and OSGi bundles built, use snippet:

```kotlin
subprojects {
    afterEvaluate {
        tasks {
            withType<AbstractArchiveTask>().configureEach {
                archiveBaseName.set("acme-${project.name}")
            }
        }
    }
}
```

Then, also common case is to customize paths in which OSGi bundles should be placed in built CRX package. As practice shows up, mostly desired snippet to be used is:

```kotlin
subprojects {
    plugins.withId("com.cognifide.aem.package") {
        configure<AemExtension> {
            `package` {
                installPath.set("/apps/acme/${project.name}/install")
            }
        }
    }
}
```

## Compatibility

| Gradle AEM Plugin | Gradle Build Tool | Adobe Experience Manager |   Java   |
|:-----------------:|:-----------------:|:------------------------:|:--------:|
|     4.x -> 5.x    |     4.x -> 4.8    |        6.x and up        |     8    |
|   6.0.0 -> 6.2.1  |     4.9 -> 5.0    |        6.x and up        |     8    |
|   6.3.0 -> 6.x    |     5.1 -> 5.6    |        6.x and up        |     8    |
|   7.2.0 -> 8.1.1  |     5.1 -> 5.6    |        6.x and up        |    8,11  |
|   8.1.2 and up    |     6.0 and up    |        6.x and up        |    8,11  |

## Building

1. Clone this project using command `git clone https://github.com/Cognifide/gradle-aem-plugin.git`
2. To build plugin, simply enter cloned directory run command: `gradlew`
3. To debug plugin under development in tests, use commands:
    * For functional tests: `sh gradlew functionalTest --debug-jvm`
    * For unit tests: `sh gradlew test --debug-jvm`
4. To debug built plugin in project when published to local Maven repository:
    * Append to any build command parameters `--no-daemon -Dorg.gradle.debug=true`
    * Run build, it will suspend, then connect remote at port 5005 by using IDE
    * Build will proceed and stop at previously set up breakpoint.

## Testing

Part of functional tests are using real AEM to ensure correctness of features.
As of AEM is not available to the public, it needs to be provided externally from remote server or by providing local file path.

AEM files available locally:

```bash
gradlew functionalTest \
-DlocalInstance.jarUrl=/Users/krystian.panek/Servers/aem65/cq-quickstart-6.5.0.jar \
-DlocalInstance.licenseUrl=/Users/krystian.panek/Servers/aem65/license.properties
```

AEM files hosted externally:

```bash
gradlew functionalTest \
-DlocalInstance.jarUrl=https://my-company.com/cq/6.5.0/cq-quickstart-6.5.0.jar \
-DlocalInstance.licenseUrl=https://my-company.com/cq/6.5.0/license.properties \
-DfileTransfer.user=foo \
-DfileTransfer.password=pass
```

## Contributing

Issues reported or pull requests created will be very appreciated. 

1. Fork plugin source code using a dedicated GitHub button.
2. Do code changes on a feature branch created from *develop* branch.
3. Create a pull request with a base of *develop* branch.

## License

**Gradle AEM Plugin** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)
