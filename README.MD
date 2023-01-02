[![WTT logo](docs/wtt-logo.png)](https://www.wundermanthompson.com/service/technology)

[![Apache License, Version 2.0, January 2004](docs/apache-license-badge.svg)](http://www.apache.org/licenses/)

<p>
  <img src="docs/logo.png" alt="Gradle AEM Plugin"/>
</p>


---

TL;DR

To add an automated AEM environment setup to the AEM Maven project [click here](https://github.com/wttech/gradle-aem-plugin/blob/main/docs/launcher.md).

---

## Table of contents

  * [About](#about)
     * [Screenshot](#screenshot)
     * [Features](#features)
     * [Compatibility](#compatibility)
  * [Getting started](#getting-started)
  * [Launcher](#launcher)
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
  * [Building](#building)
  * [Testing](#testing)
     * [Local instance tests](#local-instance-tests)
     * [Debugging tests](#debugging-tests)
  * [Contributing](#contributing)
  * [License](#license)

## About

Swiss army knife for AEM related automation. Incremental build which takes seconds, not minutes. Developer who does not loose focus between build time gaps. Extend freely your build system directly in project. 

AEM developer - it's time to meet Gradle! You liked or used plugin? Don't forget to **star this project** on GitHub :)

Be inspired by:

* watching live demos:
    * [Sling AdaptTo 2020 Conference](https://adapt.to/2020/en/schedule/aem-developers-best-friend-gradle-aem-plugin.html),
    * [Sling AdaptTo 2018 Conference](https://adapt.to/2018/en/schedule/a-better-developer-experience-for-sling-based-applications.html),
 
* reading blog articles:
    * [Gradle-powered AEM archetypes](https://wttech.blog/blog/2020/gradle-powered-aem-project-archetypes/) by [Krystian Panek](https://github.com/pun-ky),
    * [AEM Dispatcher with ease - Gradle AEM Plugins](https://wttech.blog/blog/2020/aem-dispacher-with-ease-gradle-aem-plugins/) by [Damian Mierzwiński](https://github.com/mierzwid),
    * [&amp; more...](https://wttech.blog/tag/gradle-aem-plugin/).

Looking for a dedicated version of plugin for [**Apache Sling**](https://sling.apache.org)? Check out [Gradle Sling Plugin](https://github.com/wttech/gradle-sling-plugin)!

### Demo

The example below presents building & deploying AEM package - all handled by Gradle.
To review building AEM package by Maven but all the rest handled by Gradle/GAP see [Enhancing Maven build](docs/launcher.md#enhancing-maven-build) section.

<p align="center">
  <img src="docs/all-package-deploy.gif" alt="Gradle AEM Multi Build"/>
</p>

What is being done above by simply running super easy command `sh gradlew` <=> `gw` ?

* `:env:instanceProvision` - configuring AEM instances / performing provisioning steps like:
    * deploying dependent CRX packages,
    * enabling access to CRX DE (e.g on all environments except production),
    * configuring replication agents (AEM platform wide changes),
* `:app:aem:all:packageDeploy` -> building & deploying all-in-one CRX package to AEM instances in parallel, then awaiting for stable condition of AEM instances and built application.

**Build is incremental** which guarantees optimized time every time regardless of build command used.
Only changed parts of application are processed again:

* Dependent CRX packages are installed only when they are not already installed on particular AEM instances.
* Provisioning steps are performed only once on each AEM instance (by default but customizable),
* CRX package is rebuild only when JCR content / files under *jcr_root* are changed.
* Java code is recompiled only when code in *\*.java* files is changed.
* Front-end / Webpack build is run again only when code in *\*.scss* and *\*.js* etc files is changed.

Want to see it in action? Follow [here](https://github.com/wttech/gradle-aem-multi)!

### Features 

* Automated [native AEM instances](docs/instance-plugin.md) setup optimized for best development experience.
* [Powerful AEM DSL scripting capabilities](docs/common-plugin.md#implementing-tasks) for performing JCR content migrations, managing AEM instances, integrating with Docker based tools.
* [Advanced AEM instance(s) stability & health checking](docs/instance-plugin.md#task-instanceawait) after CRX package deployment.
* [Continuous AEM incident monitoring](docs/instance-plugin.md#task-instancetail) and interactive reporting (centralized log tailing of any AEM instances with no SSH).
* Easy parallel [CRX package deployment](docs/package-plugin.md#task-packagedeploy) to many remote group of instances.
* [Fail-safe dependent CRX packages installation](docs/instance-plugin.md#task-instanceprovision) from local and remote sources using various protocols (SMB / SSH / HTTP / custom).
* [Fast JCR content synchronization](docs/package-sync-plugin.md#task-packagesync) from running AEM instances with advanced content normalization.
* [Composing CRX package](docs/package-plugin.md#task-packagecompose) from multiple separate JCR content roots, bundles.
* [Validating CRX package](docs/package-plugin.md#crx-package-validation) using seamless integration with [OakPAL tool](http://adamcin.net/oakpal).
* [All-in-one CRX packages generation](docs/package-plugin.md#assembling-packages-merging-all-in-one) (assemblies), vault filters merging etc.
* [Easy OSGi bundle customization](docs/bundle-plugin.md) with BND tool embedded.

Gradle AEM Plugin is following strategy [convention over configuration](https://en.wikipedia.org/wiki/Convention_over_configuration). When following built-in conventions about project structure & naming, then only minimal configuration is required. 
Still all features are **fully configurable**.

## Compatibility

| Gradle AEM Plugin | Gradle Build Tool | AEM On-Prem | AEMaaCS            | Java |
|-------------------|-------------------|-------------|--------------------|------|
| 4.x up to 5.x     | 4.0 up to 4.8     | 6.x and up  | not supported      | 8    |
| 6.0.0 up to 6.2.1 | 4.9 up to 5.0     | 6.x and up  | not supported      | 8    |
| 6.3.0 up to 6.x   | 5.1 up to 5.6     | 6.x and up  | not supported      | 8    |
| 7.2.0 up to 8.1.1 | 5.1 up to 5.6     | 6.x and up  | not supported      | 8,11 |
| 8.1.2 up to 13.x  | 6.0 and up        | 6.x and up  | not supported      | 8,11 |
| 14.1.0 and up     | 6.0 and up        | 6.x and up  | 2021.x and up      | 8,11 |
| 14.4.22 and up    | 6.7 and up        | 6.x and up  | 2021.x and up      | 8,11 |
| 16.0.1 and up     | 6.7 and up        | 6.x and up  | 2022.7.8005 and up | 8,11 |

Note that since GAP 14.4.22 default Java version used to compile and run AEM instances is Java 11.
To instruct GAP to use Java 8, consider setting property:

```ini
javaSupport.version=8
```

However since GAP 15.3.3, Java version automatically determined by Quickstart JAR (property below), so that Java support version does not need to be explicitly set up.

```ini
localInstance.quickstart.jarUrl=https://company-share.com/aem/cq-quicstart-6.5.0.jar
```

## Getting started

Most effective way to experience Gradle AEM Plugin is to use:
  * [GAP Launcher](docs/launcher.md#enhancing-maven-build) - recommended for adding Gradle/GAP support to existing AEM Maven builds (archetype-compatible),
  * [AEM Single-Project Example](https://github.com/wttech/gradle-aem-single#quickstart) - recommended for **application/library** development,
  * [AEM Multi-Project Example](https://github.com/wttech/gradle-aem-multi#quickstart) - recommended for **long-term project** development,
  * [AEM Boot](https://github.com/wttech/gradle-aem-boot#quickstart) - only booting local AEM instances and AEM dispatcher automatically. Useful when building CRX packages is covered separately, e.g by Maven & [Content Package Maven Plugin](https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/vlt-mavenplugin.html).
  
The only software needed on your machine to start using plugin is Java 8 or newer (also to setup local native AEM instances).
Optionally, [Docker](https://www.docker.com/) is needed (when using automatic AEM dispatcher setup).

As a build command, it is recommended to use Gradle Wrapper (`gradlew`) instead of locally installed Gradle (`gradle`) to easily have same version of build tool installed on all environments. Only at first build time, wrapper will be automatically downloaded and installed, then reused.

## Launcher

To use Gradle AEM Plugin it is not needed to have configured a regular Gradle project.
By using a single bash command, to be able to:

* set up local AEM instances and be able to share command to others even on team chat,
* download and deploy to AEM instances dependent CRX package from any source by single command run in terminal,
* copy JCR content between instances using GAP run on continuous integration server,
* and more,

consider using [standalone launcher](docs/launcher.md) as it could be the easiest and fastest way to use GAP.

## Plugins 

### Plugins setup

Released versions of plugin are available on Gradle Plugin Portal](https://plugins.gradle.org/search?term=com.cognifide.aem).
Recommended way is to apply plugin using Gradle Plugin Portal and techniques described there.

#### Minimal plugins setup

Configuration assumes:
 
* building and deploying CRX packages to AEM instance(s) via command: `gradlew packageDeploy`.
* JCR content placed under directory *src/main/content/jcr_root* 
* Vault filters located at path *src/main/content/META-INF/vault/filter.xml*,

Then the only thing needed to build CRX package is plugin application (all the rest is obtained automatically by convention):

File *build.gradle.kts*:

```kotlin
plugins {
    id("com.cognifide.aem.package") version "<version>"
}
```

#### Complete plugins setup

Illustrative configuration below assumes building and deploying on AEM instance(s) via command: `gradlew` (default tasks will be used).
Intention of snippet below is to demonstrate: 

* How particular values could be customized via Gradle AEM DSL,
* What are the default values and how they are determined.

```kotlin
plugins {
    id("com.cognifide.environment") version "<version>" // useful to setup AEM dispatcher running on Docker
    id("com.cognifide.aem.instance.local") version "<version>" // useful to setup local AEM instances running natively, skip '.local' to work with remote only
    id("org.jetbrains.kotlin.jvm") // needed when AEM code written in Kotlin, yes it could be :)
    id("com.cognifide.aem.bundle") version "<version>" // needed to built OSGi bundle
    id("com.cognifide.aem.package") version "<version>" // needed to build CRX package from JCR content and built OSGi bundle
}

group = "com.company.aem"
version = "1.0.0"
defaultTasks(":instanceProvision", ":packageDeploy")

aem {
    `package` { // built CRX package options
        contentDir.set(project.file("src/main/content"))
        appPath.set(when {
            project == project.rootProject -> "/apps/${project.rootProject.name}"
            else -> "/apps/${project.rootProject.name}/${projectName}"
        })
        nodeTypesSync("PRESERVE_AUTO")
        validator {
            enabled.set(prop.boolean("package.validator.enabled") ?: true)
            verbose.set(prop.boolean("package.validator.verbose") ?: true)
            planName.set(prop.string("package.validator.plan") ?: "plan.json")
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
            connectionTimeout.set(prop.int("instance.http.connectionTimeout") ?: 30000)
            connectionRetries.set(prop.boolean("instance.http.connectionRetries") ?: true)
            connectionIgnoreSsl.set(prop.boolean("instance.http.connectionIgnoreSsl") ?: true)
    
            proxyHost.set(prop.string("instance.http.proxyHost"))
            proxyPort.set(prop.int("instance.http.proxyPort"))
            proxyScheme.set(prop.string("instance.http.proxyScheme"))
        }

        provisioner { // configuring AEM instances in various circumstances (e.g only once)
            enableCrxDe()
            deployPackage("com.adobe.cq:core.wcm.components.all:2.11.0@zip")
            deployPackage("com.neva.felix:search-webconsole-plugin:1.3.0")
            step("setup-replication-author") {
                condition { once() && instance.author }
                sync {
                    repository {
                        save("/etc/replication/agents.author/publish/jcr:content", mapOf(
                                "enabled" to true,
                                "userId" to instance.user,
                                "transportUri" to "http://localhost:4503/bin/receive?sling:authRequestLogin=1",
                                "transportUser" to instance.user,
                                "transportPassword" to instance.password
                        ))
                    }
                }
            }
            // ...
        }  
    }
    localInstance { // config for AEM instances to be created on local file system
        quickstart {
            jarUrl.set(prop.string("localInstance.quickstart.jarUrl"))
            licenseUrl.set(prop.string("localInstance.quickstart.licenseUrl"))
        }
        backup {
            uploadUrl.set(prop.string("localInstance.backup.uploadUrl"))
            downloadUrl.set(prop.string("localInstance.backup.downloadUrl"))
        }
        install { // CRX packages and OSGi bundles to be pre-installed on created AEM instances
            files(
                "http://.../package.zip" // CRX package downloaded over HTTP
                "group:name:version" // OSGi bundle from Maven repository
            )
        }
        init { // hook called once in scope of instance just created and up first time
            logger.info("Initializing instance '$name'")
            sync {
                // ...
            }
        }   
        rootDir.set(prop.string("localInstance.rootDir"))
        // ...
    }

    tasks {
        jar { 
            bundle {
                // customizing OSGi bundle manifest
                description = "Example application built by GAP"
                docUrl = "https://github.com/wttech/gradle-aem-example"
                exportPackage("com.company.example.aem.*") 
                slingModelPackages = "com.company.example.aem"
        
                // for checking OSGi component health on runtime
                javaPackage.set("com.company.example.aem")
    
                // other / more advanced options
                importPackageWildcard.set(true)
                // ...
            }
        }
        packageCompose { // customizing built CRX package
            nestPackageProject(":core")
            nestPackageProject(":config")
            
            archiveBaseName.set("example-for-changing-zip-name")
            
            vaultDefinition { // place for overriding CRX Package / Vault properties, defining hooks
                // ...
            }
        }
        // ... and all other tasks
    }
}
```

To see all available options and actual documentation, please follow to:

* `aem` - [AemExtension](src/main/kotlin/com/cognifide/gradle/aem/AemExtension.kt)
* `package` - [PackageOptions](src/main/kotlin/com/cognifide/gradle/aem/common/pkg/PackageOptions.kt)
* `instance` - [InstanceManager](src/main/kotlin/com/cognifide/gradle/aem/common/instance/InstanceManager.kt)
* `localInstance` - [LocalInstanceManager](src/main/kotlin/com/cognifide/gradle/aem/common/instance/LocalInstanceManager.kt)
* `bundleCompose` - [BundleCompose](src/main/kotlin/com/cognifide/gradle/aem/bundle/tasks/BundleCompose.kt)
* `packageCompose` - [PackageCompose](src/main/kotlin/com/cognifide/gradle/aem/pkg/tasks/PackageCompose.kt)
* `instanceProvision` - [InstanceProvision](src/main/kotlin/com/cognifide/gradle/aem/instance/tasks/InstanceProvision.kt)
* `...` - other tasks in similar way.

### Plugins documentation

Gradle AEM Plugin to be more concise is now more like set of plugins.
Each plugin has its own documentation:

* [Common Plugin](docs/common-plugin.md) - defining common options like AEM instances available etc, base for custom Gradle tasks scripting for AEM,
* [Package Plugin](docs/package-plugin.md) - building and deploying CRX package(s),
* [Package Sync Plugin](docs/package-sync-plugin.md) - synchronizing JCR content from running AEM instances into built CRX packages,
* [Bundle Plugin](docs/bundle-plugin.md) - building and deploying OSGi bundle(s),
* [Instance Plugin](docs/instance-plugin.md) - managing remote AEM instance(s), automatic installation of service packs, performing provisioning actions,
* [Local Instance Plugin](docs/local-instance-plugin.md) - setting up local AEM instance(s),
* [Environment Plugin](https://github.com/wttech/gradle-environment-plugin) - standalone plugin for setting AEM dispatcher running on Docker,
* [Common Plugin](https://github.com/wttech/gradle-common-plugin) - standalone plugin for transferring files over protocols SFTP/SMB/HTTP, e.g downloading AEM files, uploading AEM backups.

## How to's

### Set AEM configuration properly for all / concrete project(s)

Common configuration like root of content for JCR package, should be defined in `allprojects` section like below / e.g in root *build.gradle.kts* file:

```kotlin
import com.cognifide.gradle.aem.bundle.tasks.bundle

allprojects {
  plugins.withId("com.cognifide.aem.common") {
    configure<AemExtension> {
        `package` {
            contentDir.set(project.file("src/main/aem")) // overrides default dir named 'content'
        }
    }
  }
  
  plugins.withId("com.cognifide.aem.bundle") {
    tasks {
        jar {
            bundle {
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
import com.cognifide.gradle.aem.bundle.tasks.bundle

plugins {
    id("com.cognifide.aem.bundle")
}

tasks {
    jar {
        bundle {
            javaPackage.set("com.company.example.aem.core")
        }
    }
    packageCompose {
        nestPackageProject(':content')
        archiveBaseName.set("example-core")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
```

### Understand why there are one or two plugins to be applied in build script

Gradle AEM Plugin assumes separation of 5 plugins to properly fit into Gradle tasks structure correctly.

Most often, Gradle commands are being launched from project root and tasks are being run by their name e.g `instanceStatus` (which is not fully qualified, better if it will be `:instanceStatus` of root project).
Let's imagine if task `instanceStatus` will come from package plugin, then Gradle will execute more than one `instanceStatus` (for all projects that have plugin applied), so that this is unintended behavior.
Currently used plugin architecture solves that problem.

### Work effectively on start and daily basis

Initially, to create fully configured local AEM instances simply run command `gradlew instanceSetup`.
Later during development process, building and deploying to AEM should be done using the command: `gradlew instanceProvision packageDeploy`.

* Firstly dependent packages (like AEM hotfixes, Vanity URL Components etc) will be installed lazily (only when they are not installed yet).
* In next step application is being built and deployed to all configured AEM instances.
* Finally build awaits till all AEM instances and built application are stable.

### Customize convention for CRX package and OSGi bundle names and paths

Because of [bug](https://github.com/gradle/gradle/issues/8401) related with regresion introduced in Gradle 5.1, there are some difficulties with setting archives base names.
AEM Plugin is overriding default Gradle convention for not only having project name in archive base name, but also to having prefix - root project name when project is one of subprojects (multi-project build case as in [Gradle AEM Multi](https://github.com/wttech/gradle-aem-multi/)).
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

### Target individual instances when running tasks

The ability to perform tasks against individual instances is provided by the Common Plugin, which comes with instance filtering. [Read more on instance filtering](https://github.com/wttech/gradle-aem-plugin/blob/master/docs/common-plugin.md#instance-filtering) if you're looking for information on:
  - how to destroy an individual instance,
  - how to start or stop author/publish instances only,
  - how to deploy to a single instance,
  - etc.

## Building

1. Clone this project using command `git clone https://github.com/wttech/gradle-aem-plugin.git`
2. To build plugin, simply enter cloned directory run command: `gradlew`
3. To debug plugin under development in tests, use commands:
    * For functional tests: `sh gradlew functionalTest --debug-jvm`
    * For unit tests: `sh gradlew test --debug-jvm`
4. To debug built plugin in project when published to local Maven repository:
    * Append to any build command parameters `--no-daemon -Dorg.gradle.debug=true`
    * Run build, it will suspend, then connect remote at port 5005 by using IDE
    * Build will proceed and stop at previously set up breakpoint.

## Testing

## Local instance tests

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

## Debugging tests

To debug plugin source code while:

* running functional tests, append `--debug-jvm -Porg.gradle.testkit.debug=true`.
* project using plugin, append `--no-daemon -Dorg.gradle.debug=true`.

Gradle will stop for a moment and wait until remote connection at port 5005 will be established from e.g IDE.

## Contributing

Issues reported or pull requests created will be very appreciated. 

1. Fork plugin source code using a dedicated GitHub button.
2. Do code changes on a feature branch created from *develop* branch.
3. Create a pull request with a base of *develop* branch.

## License

**Gradle AEM Plugin** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)
