![Cognifide logo](docs/cognifide-logo.png)

[![Gradle Status](https://gradleupdate.appspot.com/Cognifide/gradle-aem-plugin/status.svg?random=123)](https://gradleupdate.appspot.com/Cognifide/gradle-aem-plugin/status)
[![Apache License, Version 2.0, January 2004](docs/apache-license-badge.svg)](http://www.apache.org/licenses/)
![Travis Build](https://travis-ci.org/Cognifide/gradle-aem-plugin.svg?branch=develop)

<p>
  <img src="docs/logo.png" alt="Gradle AEM Plugin"/>
</p>

## Table of contents

  * [About](#about)
     * [Screenshot](#screenshot)
     * [Features](#features)
     * [Important notice](#important-notice)
  * [Getting started](#getting-started)
  * [Configuration](#configuration)
     * [Plugin setup](#plugin-setup)
        * [Minimal](#minimal)
        * [Additional](#additional)
     * [Common plugin](#common-plugin)
        * [Instance conventions](#instance-conventions)
        * [Defining instances via properties file](#defining-instances-via-properties-file)
        * [Defining instances via build script](#defining-instances-via-build-script)
        * [Instance URL credentials encoding](#instance-url-credentials-encoding)
     * [Tooling plugin](#tooling-plugin)
        * [Task sync](#task-sync)
           * [Cleaning features](#cleaning-features)
           * [Default cleaning configuration](#default-cleaning-configuration)
           * [Rendition cleaning configuration](#rendition-cleaning-configuration)
           * [Using alternative transfer type](#using-alternative-transfer-type)
           * [Downloading package options](#downloading-package-options)
           * [Copying or cleaning content only](#copying-or-cleaning-content-only)
           * [Filter file at custom path](#filter-file-at-custom-path)
           * [Filter roots specified explicitly](#filter-roots-specified-explicitly)
        * [Task rcp](#task-rcp)
        * [Task vlt](#task-vlt)
        * [Task debug](#task-debug)
     * [Package plugin](#package-plugin)
        * [Task packageCompose](#task-packagecompose)
           * [CRX package default configuration](#crx-package-default-configuration)
           * [Including additional OSGi bundle into CRX package](#including-additional-osgi-bundle-into-crx-package)
           * [Nesting CRX packages](#nesting-crx-packages)
           * [Assembling packages (merging all-in-one)](#assembling-packages-merging-all-in-one)
           * [Expandable properties](#expandable-properties)
        * [Task packageDeploy](#task-packagedeploy)
           * [Deploying only to desired instances](#deploying-only-to-desired-instances)
           * [Deploying options](#deploying-options)
        * [Task packageUpload](#task-packageupload)
        * [Task packageDelete](#task-packagedelete)
        * [Task packageInstall](#task-packageinstall)
        * [Task packageUninstall](#task-packageuninstall)
        * [Task packagePurge](#task-packagepurge)
        * [Task packageActivate](#task-packageactivate)
     * [Bundle plugin](#bundle-plugin)
        * [Bundle conventions](#bundle-conventions)
        * [Embedding JAR file into OSGi bundle](#embedding-jar-file-into-osgi-bundle)
        * [Configuring OSGi bundle manifest attributes](#configuring-osgi-bundle-manifest-attributes)
        * [Excluding packages being incidentally imported by OSGi bundle](#excluding-packages-being-incidentally-imported-by-osgi-bundle)
     * [Instance plugin](#instance-plugin)
        * [Instance file structure](#instance-file-structure)
        * [Task instanceSetup](#task-instancesetup)
        * [Task instanceResetup](#task-instanceresetup)
        * [Task instanceCreate](#task-instancecreate)
           * [Configuration of AEM instance source (JAR file or backup file)](#configuration-of-aem-instance-source-jar-file-or-backup-file)
           * [Extracted instance files configuration (optional)](#extracted-instance-files-configuration-optional)
        * [Task instanceBackup](#task-instancebackup)
           * [Work with remote instance backups](#work-with-remote-instance-backups)
        * [Task instanceDestroy](#task-instancedestroy)
        * [Task instanceUp](#task-instanceup)
        * [Task instanceDown](#task-instancedown)
        * [Task instanceRestart](#task-instancerestart)
        * [Task instanceReload](#task-instancereload)
        * [Task instanceResolve](#task-instanceresolve)
        * [Task instanceSatisfy](#task-instancesatisfy)
        * [Task instanceCheck](#task-instancecheck)
        * [Task instanceTail](#task-instancetail)
           * [Tailing incidents](#tailing-incidents)
           * [Tailing multiple instances](#tailing-multiple-instances)
           * [Standalone tailer tool](#standalone-tailer-tool)
     * [Environment plugin](#environment-plugin)
        * [Environment configuration](#environment-configuration)
           * [Notice for Docker on Windows](#notice-for-docker-on-windows)
           * [Notice for Docker Toolbox](#notice-for-docker-toolbox)
        * [Environment file structure](#environment-file-structure)
        * [Environment service health checks](#environment-service-health-checks)
        * [Task environmentUp](#task-environmentup)
        * [Task environmentDown](#task-environmentdown)
        * [Task environmentDev](#task-environmentdev)
  * [How to's](#how-tos)
     * [Set AEM configuration properly for all / concrete project(s)](#set-aem-configuration-properly-for-all--concrete-projects)
     * [Use lifecycle tasks](#use-lifecycle-tasks)
     * [Implement custom AEM tasks](#implement-custom-aem-tasks)
        * [Defining CRX package via code then downloading and sharing it using external HTTP endpoint](#defining-crx-package-via-code-then-downloading-and-sharing-it-using-external-http-endpoint)
        * [Calling AEM endpoints / making any HTTP requests](#calling-aem-endpoints--making-any-http-requests)
        * [Downloading CRX package from external HTTP endpoint and deploying it on desired AEM instances](#downloading-crx-package-from-external-http-endpoint-and-deploying-it-on-desired-aem-instances)
        * [Working with content repository (JCR)](#working-with-content-repository-jcr)
        * [Executing code on AEM runtime](#executing-code-on-aem-runtime)
        * [Controlling OSGi bundles and components](#controlling-osgi-bundles-and-components)
     * [Understand why there are one or two plugins to be applied in build script](#understand-why-there-are-one-or-two-plugins-to-be-applied-in-build-script)
     * [Work effectively on start and daily basis](#work-effectively-on-start-and-daily-basis)
     * [Filter instances to work with](#filter-instances-to-work-with)
     * [Know how properties are being expanded in instance or package files](#know-how-properties-are-being-expanded-in-instance-or-package-files)
  * [Known issues](#known-issues)
     * [No OSGi services / components are registered](#no-osgi-services--components-are-registered)
     * [Caching task packageCompose](#caching-task-packagecompose)
     * [Vault tasks parallelism](#vault-tasks-parallelism)
  * [Compatibility](#compatibility)
  * [Building](#building)
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

1. `:aem:instanceSatisfy` -> checking for new dependent CRX packages to be deployed (in a lazy & fail-safe manner) that could arrive to our AEM instances automatically if somebody else add it to build configuration in the meantime.
2. `:aem:assembly:full:packageDeploy` -> building & deploying all-in-one CRX package to AEM instances in parallel, then awaiting for stable condition of AEM instances and built application.
3. `:aem:environmentClean` -> cleaning AEM dispatcher cache and restarting HTTPD service / Apache Web Server.
4. `:aem:environmentCheck` -> running health checks ensuring that all AEM instances / websites are responding correctly.

**Build is incremental** which guarantees optimized time every time regardless of build command used.
Only changed parts of application are processed again:

* Dependent CRX packages are installed only when they are not already installed on particular AEM instances.
* CRX package is rebuild only when JCR content / files under *jcr_root* are changed.
* Java code is recompiled only when code in *\*.java* files is changed.
* Front-end / Webpack build is run again only when code in *\*.scss* and *\*.js* etc files is changed.

Want to see it in action? Follow [here](https://github.com/Cognifide/gradle-aem-multi)!

### Features 

* Automated complete AEM environment setup with [virtualized AEM dispatcher](#environment-plugin) and [native AEM instances](#instance-plugin) optimized for best development experience.
* [Powerful AEM DSL scripting capabilities](#implement-custom-aem-tasks) for performing JCR content migrations, managing AEM instances.
* [Advanced AEM instance(s) stability & health checking](#task-instanceawait) after CRX package deployment.
* [Continuous AEM incident monitoring](#task-instancetail) and interactive reporting (centralized log tailing of any AEM instances with no SSH).
* Easy parallel [CRX package deployment](#task-packagedeploy) to many remote group of instances.
* [Hot reloading for HTTPD / AEM dispatcher configuration files](#task-environmentdev) with health checking (web browser usage not needed).
* [Fail-safe dependent CRX packages installation](#task-instancesatisfy) from local and remote sources using various protocols (SMB / SSH / HTTP / custom).
* [Fast JCR content synchronization](#task-sync) from running AEM instances with advanced content normalization.
* [Composing CRX package](#task-packagecompose) from multiple separate JCR content roots, bundles.
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

* Most effective way to experience Gradle AEM Plugin is to use *Quickstart* located in:
  * [AEM Single-Project Example](https://github.com/Cognifide/gradle-aem-single#quickstart) - recommended for **application / library** development,
  * [AEM Multi-Project Example](https://github.com/Cognifide/gradle-aem-multi#quickstart) - recommended for **long-term project** development,
* The only software needed on your machine to start using plugin is Java 8.
* As a build command, it is recommended to use Gradle Wrapper (`gradlew`) instead of locally installed Gradle (`gradle`) to easily have same version of build tool installed on all environments. Only at first build time, wrapper will be automatically downloaded and installed, then reused.

## Configuration

### Plugin setup

Released versions of plugin are available on [Bintray](https://bintray.com/cognifide/maven-public/gradle-aem-plugin), 
so that this repository needs to be included in *buildscript* section.

#### Minimal

Configuration below assumes building and deploying CRX packages to AEM instance(s) via command: `gradlew packageDeploy`.

File *buildSrc/build.gradle.kts*:

```kotlin
repositories {
    jcenter()
}

dependencies {
    implementation("com.cognifide.gradle:aem-plugin:7.0.6")
}
```

File *build.gradle.kts*:

```kotlin
plugins {
    id("com.cognifide.aem.bundle") // or 'package' for JCR content only
}

group = "com.company.aem"
```

#### Additional

Configuration below assumes building and deploying on AEM instance(s) via command: `gradlew` (default tasks will be used).

```kotlin
plugins {
    id("com.cognifide.aem.bundle")
    id("com.cognifide.aem.instance")
    id("com.cognifide.aem.environment")
    id("org.jetbrains.kotlin.jvm") // or any other like 'java' to compile OSGi bundle
}

group = "com.company.aem"
version = "1.0.0"
defaultTasks(":instanceSatisfy", ":packageDeploy")

aem {
    `package` { // built CRX package options
        jcrRoot = aem.project.file("src/main/content")
        // ...
    }
    instance { // AEM instances to work with
        local("http://localhost:4502") // local-author
        local("http://localhost:4503") // local-publish
        remote("http://192.168.100.101:4502", "int-author")
        remote("http://192.168.100.101:4503", "int-publish")
        // etc
        
        http { // allows to customize HTTP connection to AEM instances
            connectionTimeout = aem.props.int("instance.http.connectionTimeout") ?: 30000
            connectionRetries = aem.props.boolean("instance.http.connectionRetries") ?: true
            connectionIgnoreSsl = aem.props.boolean("instance.http.connectionIgnoreSsl") ?: true
    
            proxyHost = aem.props.string("instance.http.proxyHost")
            proxyPort = aem.props.int("instance.http.proxyPort")
            proxyScheme = aem.props.string("instance.http.proxyScheme")
        }
    }
    localInstance { // config for AEM instances to be created on local file system
        quickstart {
            jarUrl = aem.props.string("localInstance.quickstart.jarUrl")
            licenseUrl = aem.props.string("localInstance.quickstart.licenseUrl")
        }
        backup {
            uploadUrl = aem.props.string("localInstance.backup.uploadUrl")
            downloadUrl = aem.props.string("localInstance.backup.downloadUrl")
        }
        rootDir = aem.props.string("localInstance.root")
        // ...
    }
    environment { // config for AEM environment running on Docker
        rootDir = aem.props.string("environment.rootDir")?.let { aem.project.file(it) } ?: aem.projectMain.file(".aem/environment")
        hosts { // domains to be appended to hosts file automatically
            // ...
        }
        distributions {  // extra files for Docker containers that are missing in images like AEM dispatcher HTTPD module
            // ...
        }
        directories { // dirs for volumes that must exist before running Docker containers
            // ...
        }
        healthChecks { // checks (e.g GET requests) verifying running Docker containers like HTTPD
            // ...
        }
    }
    fileTransfer { // config for resolving CRX packages, AEM Quickstart files and backups using HTTP/SFTP/SMB
        sftp {
            user = props.string("fileTransfer.sftp.user")
            password = props.string("fileTransfer.sftp.password")
        }
        smb {
            user = props.string("fileTransfer.smb.user")
            password = props.string("fileTransfer.smb.password")
            domain = props.string("fileTransfer.smb.domain")
        }
        http {
            client {
                basicUser = props.string("fileTransfer.http.user")
                basicPassword = props.string("fileTransfer.http.password")
            }
        }
        
        credentials("foo", "bar") // shorthand to set all user / password pairs above
    }
    tasks {
        bundle { // customizing OSGi bundle manifest
            javaPackage = "com.company.example.aem"
            // ...
        }
        packageCompose { // customizing built CRX package
            fromProject(":core")
            fromProject(":config")
            
            baseName = 'example-for-changing-zip-name'
            
            vaultDefinition { // place for overriding CRX Package / Vault properties
                // ...
            }
        }
        instanceSatisfy { // customizing CRX packages to be deployed as dependencies before built AEM application
            packages {
                download("http://.../package.zip")
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
* `bundle` - [BundleJar](src/main/kotlin/com/cognifide/gradle/aem/bundle/BundleJar.kt)
* `localInstance` - [LocalInstanceManager](src/main/kotlin/com/cognifide/gradle/aem/instance/LocalInstanceManager.kt)
* `environment` - [Environment](src/main/kotlin/com/cognifide/gradle/aem/environment/Environment.kt)
* `fileTransfer` - [FileTransferManager](src/main/kotlin/com/cognifide/gradle/aem/common/file/transfer/FileTransferManager.kt)
* `packageCompose` - [PackageCompose](src/main/kotlin/com/cognifide/gradle/aem/pkg/tasks/PackageCompose.kt)
* `instanceSatisfy` - [InstanceSatisfy](src/main/kotlin/com/cognifide/gradle/aem/instance/satisfy/InstanceSatisfy.kt)
* `...` - other tasks in similar way.

### Common plugin

```kotlin
plugins {
    id("com.cognifide.aem.common")
}
```

Applied transparently by other plugins. Provides AEM extension to build script / **AEM Gradle DSL** which consists of instance definitions, common configuration, methods for controlling local instances and virtualized environment.

It does not provide any tasks. Apply other plugins to have tasks or implement own.

#### Instance conventions

* Instance **name** is a combination of *${environment}-${id}* e.g *local-author*, *integration-publish* etc.
* Instance **type** indicates physical type of instance and could be only: *local* and *remote*. Local means that instance could be created by plugin automatically under local file system.
* Instance **id** is an instance purpose identifier and must start with prefix *author* or *publish*. Sample valid names: *author*, *author1*, *author2*, *author-master* and *publish*, *publish1* *publish2* etc.
* Only instances defined as *local* are considered in command `instanceSetup`, `instanceCreate`, `instanceUp` etc (that comes from `com.cognifide.aem.instance` plugin).
* All instances defined as *local* or *remote* are considered in commands CRX package deployment related like `instanceSatisfy`, `packageDeploy`, `packageUpload`, `packageInstall` etc.

Instances could be defined in two ways, via:
 
* file `gradle.properties` - recommended approach, by properties convention.
* build script - dynamic & more customizable approach.

#### Defining instances via properties file

The configuration could be specified through *gradle.properties* file using dedicated syntax.

`instance.$ENVIRONMENT-$ID.$PROP_NAME=$PROP_VALUE`

Part | Possible values | Description |
--- | --- | --- |
`$ENVIRONMENT` | `local`, `int`, `stg` etc | Environment name. |
`$ID` | `author`, `publish`, `publish2`, etc | Combination of AEM instance type and semantic suffix useful when more than one of instance of same type is being configured. |
`$PROP_NAME=$PROP_VALUE` | **Local instances:** `httpUrl=http://admin:admin@localhost:4502`<br>`type=local`(or remote)<br>`password=foo`<br>`runModes=nosamplecontent`<br>`jvmOpts=-server -Xmx2048m -XX:MaxPermSize=512M -Djava.awt.headless=true`, `startOpts=...`<br>`debugPort=24502`.<br><br>**Remote instances:** `httpUrl`, `type`, `user`, `password`. | Run modes, JVM opts and start opts should be comma delimited. |


Default remote instances defined via properties (below lines are optional):

```
instance.local-author.httpUrl=http://localhost:4502
instance.local-publish.httpUrl=http://localhost:4503
```

Example for defining multiple remote instances (that could be [filtered](#filter-instances-to-work-with)):

```
instance.int-author.httpUrl=http://author.aem-integration.company.com
instance.int-publish.httpUrl=http://aem-integration.company.com
instance.stg-author.httpUrl=http://author.aem-staging.company.com
instance.stg-publish.httpUrl=http://aem-staging.company.com
```

Example for defining remote instance with credentials separated:

```
instance.test-author.httpUrl=http://author.aem-integration.company.com
instance.test-author.user=foo
instance.test-author.password=bar
```

Example for defining remote instance with credentials details included in URL:

```
instance.test-author.httpUrl=http://foo:bar@author.aem-integration.company.com
```

Example for defining local instances (created on local file system):

```
instance.local-author.httpUrl=http://localhost:4502
instance.local-author.type=local
instance.local-author.runModes=nosamplecontent
instance.local-author.jvmOpts=-server -Xmx1024m -XX:MaxPermSize=256M -Djava.awt.headless=true

instance.local-publish.httpUrl=http://localhost:4503
instance.local-publish.type=local
instance.local-publish.runModes=nosamplecontent
instance.local-publish.jvmOpts=-server -Xmx1024m -XX:MaxPermSize=256M -Djava.awt.headless=true
```

Notice! Remember to define also AEM [source files](#configuration-of-aem-instance-source-jar-file-or-backup-file).

#### Defining instances via build script

Example usage below. The commented value is an effective instance name.

```kotlin
aem {
    instance {
        local("http://localhost:4502") // local-author
        local("http://localhost:4502") { // local-author
            password = "admin"
            id = "author"
            debugPort = 14502 
        }
      
        local("http://localhost:4503") // local-publish
        local("http://localhost:4503") { // local-publish
            password = "admin"
            id = "publish"
            debugPort = 14503
        } 
      
        remote("http://192.168.10.1:4502") { // integration-author1
            user = "user1" 
            password = "password2"
            environment = "integration"
            id = "author1"
        } 
        remote("http://192.168.10.1:8080") { // integration-author2
            user = "user1" 
            password = "password2"
            environment = "integration"
            id = "author2"
        } 
        remote("http://192.168.10.2:4503") { // integration-publish1
            user = "user2"
            password = "password2"
            environment = "integration"
            id = "publish1"
        } 
        remote("http://192.168.10.2:8080") { // integration-publish2
            user = "user2"
            password = "password2"
            environment = "integration"
            id = "publish2"
        } 
    }
}
```

### Instance URL credentials encoding

Remember to encode instance credentials (user & password) when passing it via properties: `instance.$INSTANCE_NAME.httpUrl` or `instance.list=[$INSTANCE_HTTP_URL1,$INSTANCE_HTTP_URL2,...]`.

For example, let's assume that instance is created by [instance plugin](#instance-plugin) using following properties:

```ini
instance.local-author.httpUrl=http://localhost:4502
instance.local-author.type=local
instance.local-author.password=gxJMge@6F5ZV#s9j
```

Password is generated by e.g [strong password generator](https://passwordsgenerator.net/) so it has special URL characters.
Then to be able to use this created instance but remotely and via `instance.list` property on CI, then it should look as below: 

```ini
sh gradlew :aem:assembly:full:packageDeploy -Pinstance.list=[http://admin:gxJMge%406F5ZV%23s9j@localhost:192.168.123.123]
```

Notice that when password is specified as separate `instance.$INSTANCE_NAME.password` property it does not need to be encoded.
Otherwise, password value must be encoded by e.g [online URL encoder](https://meyerweb.com/eric/tools/dencoder/).

### Tooling plugin

```kotlin
plugins {
    id("com.cognifide.aem.tooling")
}
```

Applied transparently by package and bundle plugins. Provides tooling related tasks like `sync`, `rcp`, `debug` etc.

#### Task `sync`

Check out then clean JCR content. 

JCR content to be synchronized from AEM instance repository is determined by [workspace filter file](https://jackrabbit.apache.org/filevault/filter.html) located at path *src/main/content/META-INF/vault/sync.xml*.
However, when this file does not exist, plugin as a fallback will use same file used when composing CRX package which is *src/main/content/META-INF/vault/filter.xml*.

To exclude some files from synchronization, simply copy *filter.xml* file and create file named *sync.xml*. Then add exclusion in a following way:

```xml
<filter root="/content/dam/example">
    <exclude pattern=".*\.gif"/>
</filter>
```

##### Cleaning features

Cleaning assumes advanced JCR content normalization to minimize changes visible in VCS after each synchronization.

* unwanted JCR properties removal (with path based inclusion / exclusion rules),
* unwanted JCR mixin types removal,
* unwanted files removal,
* unused XML namespaces removal,
* flattening files (renaming e.g *_cq_dialog/.content.xml* to *_cq_dialog.xml*),
* preserving state of parent files for each Vault filter root (by backup mechanism),
* hooks for custom cleaning rules / processing *.content.xml* files.

##### Default cleaning configuration

```kotlin
aem {
    tasks {
        sync {
            cleaner {
                filesDotContent = { 
                    include("**/.content.xml") 
                }
                filesDeleted = { 
                    include("**/.vlt", "**/.vlt*.tmp") 
                }
                filesFlattened = { 
                    include("**/_cq_dialog/.content.xml", "**/_cq_htmlTag/.content.xml") 
                }
                propertiesSkipped = listOf(
                    pathRule("jcr:uuid", listOf("**/home/users/*", "**/home/groups/*")),
                    "jcr:lastModified*",
                    "jcr:created*",
                    "jcr:isCheckedOut",
                    "cq:lastModified*",
                    "cq:lastReplicat*",
                    "dam:extracted",
                    "dam:assetState",
                    "dc:modified",
                    "*_x0040_*"
                )
                mixinTypesSkipped = listOf(
                    "cq:ReplicationStatus",
                    "mix:versionable"
                )
                namespacesSkipped = true
                parentsBackupEnabled = true
                parentsBackupSuffix = ".bak"
                lineProcess = { file, line -> normalizeLine(file, line) }
                contentProcess = { file, lines -> normalizeContent(file, lines) }
            }
        }
    }
}
```

##### Rendition cleaning configuration

Cleaning could also ensure that AEM renditions will be never saved in VCS. Also any additional properties could be cleaned.
For such cases, see configuration below:

```kotlin
aem {
    tasks {
        sync {
            cleaner {
                propertiesSkipped += listOf(
                        pathRule("dam:sha1", listOf(), listOf("**/content/dam/*.svg/*")),
                        pathRule("dam:size", listOf(), listOf("**/content/dam/*.svg/*")),
                        "cq:name",
                        "cq:parentPath",
                        "dam:copiedAt",
                        "dam:parentAssetID",
                        "dam:relativePath"
                )
                filesDeleted = { 
                    include(
                        "**/.vlt",
                         "**/.vlt*.tmp",
                        "**/content/dam/**/_jcr_content/folderThumbnail*",
                        "**/content/dam/**/_jcr_content/renditions/*"
                    )
                }
            }  
        }
    }
}
```

##### Using alternative transfer type

Available transfer types: *package_download* (default) and *vlt_checkout*.

```bash
gradlew :site.demo:sync -Psync.type=vlt_checkout
```

##### Downloading package options

When transfer type is set to *package_download* then it is also possible to...

Download package only without extracting:

```bash
gradlew :site.demo:sync -PpackageDownload.extract=false
```

Download, delete all previous JCR root contents then extract fresh content:

```bash
gradlew :site.demo:sync -Pforce
```

##### Copying or cleaning content only

Available mode types: *copy_and_clean* (default), *clean_only* and *copy_only*.

```bash
gradlew :site.demo:sync -Psync.mode=clean_only
```

##### Filter file at custom path
   
```bash
gradlew :site.demo:sync -Pfilter.path=custom-filter.xml
gradlew :site.demo:sync -Pfilter.path=src/main/content/META-INF/vault/custom-filter.xml
gradlew :site.demo:sync -Pfilter.path=C:/aem/custom-filter.xml
```

##### Filter roots specified explicitly
   
```bash
gradlew :site.demo:sync -Pfilter.roots=[/etc/tags/example,/content/dam/example]
```

#### Task `rcp`

Copy JCR content from one instance to another. Sample usages below.

* Using predefined instances with multiple different source and target nodes:

  ```bash
  gradlew :rcp -Prcp.source.instance=int-author -Prcp.target.instance=local-author -Prcp.paths=[/content/example-demo=/content/example,/content/dam/example-demo=/content/dam/example]
  ```

* Using predefined instances with multiple same source and target nodes:

  ```bash
  gradlew :rcp -Prcp.source.instance=stg-author -Prcp.target.instance=int-author -Prcp.paths=[/content/example,/content/example2]
  ```
  Right side of assignment could skipped if equals to left (same path on both source & target instance).

* Using predefined instances with source and target nodes specified in file:

  ```bash
  gradlew :rcp -Prcp.source.instance=int-author -Prcp.target.instance=local-author -Prcp.pathsFile=paths.txt
  ```

  File format:
 
  ```
   sourcePath1=targetPath1
   sameSourceAndTargetPath1
   sourcePath2=targetPath2
   sameSourceAndTargetPath2
  ```


* Using dynamically defined instances:

  ```bash
  gradlew :rcp -Prcp.source.instance=http://user:pass@192.168.66.66:4502 -Prcp.target.instance=http://user:pass@192.168.33.33:4502 -Prcp.paths=[/content/example-demo=/content/example]
  ```

Keep in mind, that copying JCR content between instances, could be a trigger for running AEM workflows like *DAM Update Asset* which could cause heavy load on instance.
Consider disabling AEM workflow launchers before running this task and re-enabling after.

RCP task is internally using [Vault Remote Copy](http://jackrabbit.apache.org/filevault/rcp.html) which requires bundle *Apache Sling Simple WebDAV Access to repositories (org.apache.sling.jcr.webdav)* present in active state on instance.

#### Task `vlt`

Execute any JCR File Vault command. 

For instance, to reflect `rcp` functionality, command below could be executed:

```bash
gradlew :site.demo:sync -Pvlt.command='rcp -b 100 -r -u -n http://admin:admin@localhost:4502/crx/-/jcr:root/content/dam/example http://admin:admin@localhost:4503/crx/-/jcr:root/content/dam/example' 
```

For more details about available parameters, please visit [VLT Tool documentation](https://helpx.adobe.com/experience-manager/6-4/sites/developing/using/ht-vlttool.html).

While using task `sync` be aware that Gradle requires to have working directory with file *build.gradle.kts* in it, but Vault tool can work at any directory under *jcr_root*. To change working directory for Vault, use property `aem.vlt.path` which is relative path to be appended to *jcr_root* for project task being currently executed.

#### Task `debug` 

Dumps effective AEM build configuration of concrete project to JSON file.

When command below is being run (for root project `:`):

```bash
gradlew :debug
```

Then file at path *build/aem/debug/debug.json* with content below is being generated:

```javascript
{
  "buildInfo": {
    "plugin": {
      "pluginVersion": "7.0.0",
      "gradleVersion": "5.4"
    },
    "gradle": {
      "version": "5.4.1",
      "homeDir": ".../gradle-5.4.1"
    },
    "java": {
      "version": "1.8",
      "homeDir": ".../jdk1.8.0_121/jre"
    }
  },
  "projectInfo": {
    "displayName": "project ':aem:assembly:full'",
    "path": ":aem:assembly:full",
    "name": "full",
    "dir": ".../gradle-aem-multi/aem/assembly/full"
  },
  "packageDeployed": {
    "local-author": {
      "group": "example",
      "name": "example.full",
      "version": "1.0.0-SNAPSHOT",
      "path": "/etc/packages/example/example.full-1.0.0-SNAPSHOT.zip",
      "downloadName": "example.full-1.0.0-SNAPSHOT.zip",
      "lastUnpacked": 1558622734756,
      "installed": true
    },
    // ...
  },
  "aem": {
    "projectPrefixes": [
      "aem.",
      "aem-",
      "aem_"
    ],
    "env": "local",
    "lineSeparator": "LF",
    "fileTransfer": {
      "http": {
        "enabled": true,
        "client": {
          "baseUrl": "",
          "basicUser": null,
          "basicPassword": null,
          "authorizationPreemptive": false,
          "connectionTimeout": 30000,
          "connectionIgnoreSsl": true,
          "connectionRetries": true,
          "responseChecks": true
        },
        "name": "httpd",
        "protocols": [
          "http://*",
          "https://*"
        ]
      },
      "sftp": {
        "enabled": true,
        "user": null,
        "password": null,
        "timeout": 60000,
        "name": "sftp",
        "protocols": [
          "sftp://*"
        ]
      },
      // ...
    },
    "packageOptions": {
      "rootDir": ".../gradle-aem-multi/aem/assembly/full/src/main/content",
      "snapshots": [],
      "metaCommonRootDir": ".../gradle-aem-multi/aem/gradle/package/META-INF",
      "installPath": "/apps/example/full/install",
      "installRepository": true,
      "errors": [
        "javax.jcr.nodetype.*Exception",
        "org.apache.jackrabbit.oak.api.*Exception",
        "org.apache.jackrabbit.vault.packaging.*Exception",
        "org.xml.sax.*Exception"
      ],
      "responseBuffer": 4096
    },
    "instanceOptions": {
      "defined": {
        "local-author": {
          "properties": {},
          "httpUrl": "http://localhost:4502",
          "user": "admin",
          "password": "*****",
          "id": "author",
          "environment": "local",
          "debugPort": 14502,
          "name": "local-author",
          "type": "AUTHOR",
          "httpPort": 4502,
          "runModes": "author,local",
          "startOpts": "",
          "jvmOpts": "-Xdebug -Xrunjdwp:transport=dt_socket,address=14502,server=y,suspend=n -server -Xmx2048m -XX:MaxPermSize=512M -Djava.awt.headless=true"
        }
        // ...
      }
    },
    "localInstanceManager" : {
      "rootDir" : "*/minimal/.aem/instance",
      "source" : "AUTO",
      "overridesDir" : ".../gradle-aem-multi/gradle/instance",
      "expandFiles" : [
        "**/start.bat",
        "**/stop.bat",
        "**/start",
        "**/stop"
      ],
      "expandProperties" : { },
      "quickstart": {
        "jarUrl": null,
        "licenseUrl": null
      },
      "backup": {
        "uploadUrl": null,
        "downloadUrl": null,
        "downloadDir": ".../gradle-aem-multi/build/aem/instanceBackup/remote"
      }
    },
    "environment": {
      "rootDir": ".../gradle-aem-multi/aem/.aem/environment",
      "directories": {
        "regulars": [],
        "caches": []
      },
      "dockerRuntime": {
          "name": "desktop",
          "hostIp": "127.0.0.1"
      },
      "hosts": {
        "defined": []
      },
      "dockerComposeFile": ".../gradle-aem-multi/aem/.aem/environment/docker-compose.yml",
      "dockerComposeSourceFile": ".../gradle-aem-multi/aem/gradle/environment/docker-compose.yml.peb",
      "dockerConfigPath": ".../gradle-aem-multi/aem/gradle/environment",
      "dockerRootPath": ".../gradle-aem-multi/aem/.aem/environment",
      "dispatcherModuleFile": ".../gradle-aem-multi/aem/.aem/environment/distributions/mod_dispatcher.so",
      "configDir": ".../gradle-aem-multi/aem/gradle/environment",
      "created": true,
      "httpdConfDir": ".../gradle-aem-multi/aem/gradle/environment/httpd/conf"
    },
    "notifier": {
      "enabled": true
    },
    "tasks": {
      "bundles": []
    },
    "projectName": "full",
    "baseName": "example.full",
    "configCommonDir": ".../gradle-aem-multi/aem/gradle",
    "configDir": ".../gradle-aem-multi/aem/assembly/full/gradle",
    "javaPackages": [
      "com.company.example.aem.common",
      "com.company.example.aem.sites"
    ]
  }
}
```

### Package plugin

```kotlin
plugins {
    id("com.cognifide.aem.package")
}
```

Should be applied to all projects that are composing CRX packages from *JCR content only*.

Provides CRX package related tasks: `packageCompose`, `packageDeploy`, `packageActivate`, `packagePurge` etc.

Inherits from [Common Plugin](#common-plugin).

#### Task `packageCompose`

Compose CRX package from JCR content and bundles. 

Inherits from task [ZIP](https://docs.gradle.org/3.5/dsl/org.gradle.api.tasks.bundling.Zip.html).

##### CRX package default configuration

```kotlin
aem {
    tasks {
        packageCompose {
            duplicatesStrategy = DuplicatesStrategy.WARN
            baseName = aem.baseName
            contentDir = aem.packageOptions.rootDir
            bundlePath = aem.packageOptions.installPath
            bundleRunMode = null
            metaDefaults = true
            vaultDefinition {
                properties = mapOf(
                    "acHandling" to "merge_preserve",
                    "requiresRoot" to false
                )
                name = aem.baseName
                group = if (aem.project == aem.project.rootProject) {
                    aem.project.group.toString()
                } else {
                    aem.project.rootProject.name
                }
                version = project.version.toString()
            }
            fileFilter {
                expanding = true
                expandFiles = listOf(
                    "**/META-INF/*.xml",
                    "**/META-INF/*.MF",
                    "**/META-INF/*.cnd"
                )
                excluding = true
                excludeFiles = listOf(
                       "**/.gradle",
                       "**/.git",
                       "**/.git/**",
                       "**/.gitattributes",
                       "**/.gitignore",
                       "**/.gitmodules",
                       "**/.vlt",
                       "**/.vlt*.tmp",
                       "**/node_modules/**",
                       "jcr_root/.vlt-sync-config.properties"
               )
               bundleChecking = true
            }
            merging {
                vaultFilters = true
            }
            fromConvention = true
        }
    }    
}
```

##### Including additional OSGi bundle into CRX package

Use dedicated task method named `fromJar`, for example:

```kotlin
aem {
    tasks {
        packageCompose {
            fromJar("com.github.mickleroy:aem-sass-compiler:1.0.1)
        }
    }
}
```

For reference, see usage above in [AEM Multi-Project Example](https://github.com/Cognifide/gradle-aem-multi/blob/master/aem/common/build.gradle.kts).

##### Nesting CRX packages

Use dedicated task method named `fromZip`, For example:

```kotlin
aem {
    tasks {
        packageCompose {
            fromZip("com.adobe.cq:core.wcm.components.all:2.4.0")
            fromZip("com.adobe.cq:core.wcm.components.examples:2.4.0")
        }
    }
}
```

##### Assembling packages (merging all-in-one)

Let's assume following project structure:

* *aem/build.gradle.kts* (project `:aem`, no source files at all)
* *aem/sites/build.gradle.kts*  (project `:aem:sites`, JCR content and OSGi bundle)
* *aem/common/build.gradle.kts*  (project `:aem:common`, JCR content and OSGi bundle)
* *aem/site.live/build.gradle.kts*  (project `:aem:site.live`, JCR content only)
* *aem/site.demo/build.gradle.kts*  (project `:aem:site.demo`, JCR content only)

File content of *aem/build.gradle.kts*:

```kotlin
plugins {
    id("com.cognifide.aem.package")
}

aem {
    tasks {
        packageCompose {
            fromProject(":aem:sites")
            fromProject(":aem:common")
            fromProjects(":aem:site.*")
        }
    }    
}
```

When building via command `gradlew :aem:build`, then the effect will be a CRX package with assembled JCR content and OSGi bundles from projects: `:aem:sites`, `:aem:common`, `:aem:site.live`, `:aem:site.demo`.

Gradle AEM Plugin is configured in a way that project can have:
 
* JCR content,
* source code to compile OSGi bundle,
* both.

By distinguishing `fromProject`, `fromBundle` or `fromCompose` there is ability to create any assembly CRX package with content of any type without restructuring the project.

When using `fromProject` there is an ability to pass lambda to customize options like `bundlePath`, `bundleRunMode`, decide to include only JCR contents, only bundles and more.

However, one rule must be kept while developing a multi-module project: **all Vault filter roots of all projects must be exclusive**. In general, they are most often exclusive, to avoid strange JCR installer behaviors, but sometimes exceptional [workspace filter](http://jackrabbit.apache.org/filevault/filter.html) rules are being applied like `mode="merge"` etc.

##### Expandable properties

In exactly the same way as it works for instance files, properties can be expanded inside metadata files of package being composed.

Related configuration:

```kotlin
aem {
    tasks {
        packageCompose {
            fileFilter {
                expandProperties = mapOf(
                    "organization" to "Company"
                )
                expandFiles = listOf(
                    "**/META-INF/*.xml",
                    "**/META-INF/*.MF",
                    "**/META-INF/*.cnd"
                )
            }
        }
    }
}
```

Predefined expandable properties:

* `aem` - [AemExtension](src/main/kotlin/com/cognifide/gradle/aem/AemExtension.kt) object,
* `definition` - [VltDefinition](src/main/kotlin/com/cognifide/gradle/aem/common/pkg/vlt/VltDefinition.kt) object,
* `rootProject` - project with directory in which *settings.gradle* is located,
* `project` - current project.

This feature is especially useful to generate valid *META-INF/properties.xml* file, below [template](src/main/resources/com/cognifide/gradle/aem/package/META-INF/vault/properties.xml) is used by plugin by default:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
    {% if definition.description is not empty %}
    <comment>{{definition.description}}</comment>
    <entry key="description">{{definition.description}}</entry>
    {% endif %}
    <entry key="group">{{definition.group}}</entry>
    <entry key="name">{{definition.name}}</entry>
    <entry key="version">{{definition.version}}</entry>
    {% if definition.createdBy is not empty %}
    <entry key="createdBy">{{definition.createdBy}}</entry>
    {% endif %}
    {% for e in definition.properties %}
    <entry key="{{e.key}}">{{e.value | raw}}</entry>
    {% endfor %}
</properties>
```

Also file *nodetypes.cnd* is dynamically expanded from [template](src/main/resources/com/cognifide/gradle/aem/package/META-INF/vault/nodetypes.cnd) to generate file containing all node types from all sub packages being merged into assembly package.

Each JAR file in separate *hooks* directory will be combined into single directory when creating assembly package.

#### Task `packageDeploy` 

Upload & install CRX package into AEM instance(s). Primary, recommended form of deployment. Optimized version of `packageUpload packageInstall`.

##### Deploying only to desired instances

Simply use generic approach for [filtering instances to work with](#filter-instances-to-work-with).

##### Deploying options

Add any of below command line parameters to customize CRX package deployment behavior:

* `-Ppackage.deploy.awaited=false` - disable stability & health checks after deploying CRX package.
* `-Ppackage.deploy.distributed=true` - use alternative form of deployment. At first, deploys CRX package to author instances, then triggers replication of CRX package so that it will be installed also on publish instances.
* `-Ppackage.deploy.uploadForce=false` - disable force installation (by default even unchanged CRX package is forced to be reinstalled)
* `-Ppackage.deploy.installRecursive=false` - disable automatic installation of subpackages located inside CRX package being deployed.  
* `-Ppackage.deploy.uploadRetry=n` - customize number of retries being performed after failed CRX package upload.
* `-Ppackage.deploy.installRetry=n` - customize number of retries being performed after failed CRX package install.

#### Task `packageUpload`

Upload composed CRX package into AEM instance(s).

#### Task `packageDelete`

Delete uploaded CRX package from AEM instance(s).

#### Task `packageInstall`

Install uploaded CRX package on AEM instance(s).

#### Task `packageUninstall`

Uninstall uploaded CRX package on AEM instance(s).

To prevent data loss, this unsafe task execution must be confirmed by parameter `-Pforce`.

#### Task `packagePurge` 

Fail-safe combination of `packageUninstall` and `packageDelete`.

To prevent data loss, this unsafe task execution must be confirmed by parameter `-Pforce`.

#### Task `packageActivate` 

Replicate installed CRX package to other AEM instance(s).

### Bundle plugin

Should be applied to all projects that are composing CRX packages from both *OSGi bundle* being built and optionally *JCR content*. 

Inherits from [Package Plugin](#package-plugin).

#### Bundle conventions

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
        bundle {
            javaPackage = "${project.group}.${project.name}" // "com.company.example.aem.sites"
        }
    }
}
```

#### Embedding JAR file into OSGi bundle

Use dedicated method in *bundle* section.

```kotlin
aem {
    tasks {
        bundle {
            embedPackage("com.group.name",  true, 'group:name:version') // true -> exportPackage, false -> privatePackage
        }
    }
}
```
 
For the reference, see [usage in AEM Multi-Project Example](https://github.com/Cognifide/gradle-aem-multi/blob/master/aem/common/build.gradle.kts).

#### Configuring OSGi bundle manifest attributes

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
        bundle {
            displayName = 'My Bundle"
            symbolicName = "com.company.aem.example.common"
            slingModelPackages = "com.company.aem.example.common.models"
            exportPackage("com.company.aem.example.common")
        }
    }
}
```

#### Excluding packages being incidentally imported by OSGi bundle

Sometimes BND tool could generate *Import-Package* directive that will import too many OSGi classes to be available on bundle class path. Especially when we are migrating non-OSGi dependency to OSGi bundle (because of transitive class dependencies).
 
To prevent that we could generate own manifest entry that will prevent importing optional classes.

For instance: 

```kotlin
aem {
    tasks {
        bundle {
            excludePackages("org.junit", "org.mockito")
            importPackages("!org.junit", "!org.mockito", "*") // alternatively
        } 
    }
}
```

### Instance plugin

```kotlin
plugins {
    id("com.cognifide.aem.instance")
}
```

Provides instance related tasks: `instanceCheck`, `instanceSetup`, `instanceCreate` etc.

Should be applied only at root project / only once within whole build.

Inherits from [Common Plugin](#common-plugin).

#### Instance file structure

By default, instance file are stored directly in project, under so called main AEM module usually named *aem*.
Ensure having directory *aem/.instance* ignored in VCS and excluded from indexing by IDE.

![Instance file structure](docs/instance-file-structure.png)

#### Task `instanceSetup`

Performs initial setup of local AEM instance(s). Automated version of `instanceCreate instanceUp instanceSatisfy packageDeploy`.

#### Task `instanceResetup`

Combination of `instanceDown instanceDestroy instanceSetup`. Allows to quickly back to initial state of local AEM instance(s).

To prevent data loss, this unsafe task execution must be confirmed by parameter `-Pforce`.

#### Task `instanceCreate`
 
Create AEM instance(s) at local file system. Extracts *crx-quickstart* from downloaded JAR and applies configuration according to [instance definitions](#defining-instances-via-properties-file). 

##### Configuration of AEM instance source (JAR file or backup file)

To create instances from scratch, specify:

```ini
localInstance.quickstart.jarUrl=[protocol]://[user]:[password]@[host]/[path]/cq-quickstart.jar
localInstance.quickstart.licenseUrl=[protocol]://[user]:[password]@[host]/[path]/license.properties
```

To create instances from local backups, firstly create instances from scratch, then run [backup task](#task-instancebackup). After creating backup, at any time, instances could be restored to previous state by running [resetup task](#task-resetup).
Nothing need to be configured by default.

To create instances from remote backups see section [work with remote instance backups](#work-with-remote-instance-backups).

By default plugin tries to automatically find most recent backup from all available sources. 
However to e.g avoid creating instances from the scratch accidentally, source mode can be adjusted by specifying property `localInstance.source`.
Available values:

* `auto` - Create instances from most recent backup (external or internal) or fallback to creating from the scratch if there is no backup available.
* `scratch` - Force creating instances from the scratch.
* `backup_any` - Force using any backup available at local or remote source.
* `backup_remote` - Force using backup available at remote source (specified as `localInstance.backup.[downloadUrl|uploadUrl]`).      
* `backup_local` - Force using local backup (created by task `instanceBackup`).

When mode is different than `scratch`, then backup ZIP file selection rule could be adjusted:

```kotlin

aem {
    localInstance {
        backup {
            selector = {  // default implementation below
                val sorted = sortedWith(compareByDescending<BackupSource> { it.fileEntry.name }.thenBy { it.type.ordinal })
                val name = aem.props.string("localInstance.backup.name") ?: ""
        
                when {
                    name.isNotBlank() -> sorted.firstOrNull { it.fileEntry.name == name }
                    else -> sorted.firstOrNull()
                }
            }
        }
    }
}

```

Notice that, default selector assumes that most recent backup will be selected.
Ordering by file name including timestamp then local backups precedence when backup is available on both local & remote source.
Still, backup selector could select exact backup by name when property `localInstance.backup.name` is specified.

##### Extracted instance files configuration (optional)

Plugin allows to override or provide extra files to local AEM instance installations.
This behavior is controlled by:

```kotlin
aem {
    localInstance {
        rootDir = aem.props.string("localInstance.root")?.let { aem.project.file(it) } ?: aem.projectMain.file(".aem/instance")
        overridesDir = File(aem.configCommonDir, LocalInstance.FILES_PATH)
        expandProperties = mapOf()
        expandFiles = listOf(
            "**/*.properties", 
            "**/*.sh", 
            "**/*.bat", 
            "**/*.xml",
            "**/start",
            "**/stop"
        )
    }
}
```

Properties:

* *rootDir* determines where AEM instance files will be extracted on local file system.
* *overridesDir* determines project location that holds extra instance files that will override plugin defaults (start / stop scripts) and / or extracted AEM files.
* *expandProperties* is a place for defining custom properties that can be expanded in AEM instance files.
* *expandFiles* specifies which AEM instance files have an ability to use [expandable properties](#expandable-properties) inside.

Predefined expandable properties:

* `instance` - [LocalInstance](src/main/kotlin/com/cognifide/gradle/aem/common/instance/LocalInstance.kt) object.

#### Task `instanceBackup`

Turns off local AEM instance(s) then archives them into ZIP file, then turns on again.

The most recent file created by this task will be reused automatically while running task `instanceCreate`.

Backup files are stored at path relative to project that is applying plugin `com.cognifide.aem.instance`.
Most often it will be path: *build/aem/instanceBackup/local/xxx.backup.zip*. It could be overridden by writing:

```kotlin
aem {
    tasks {
        backup {
            destinationDirectory.set(file("any/other/path"))
        }
    }
}
```

Instance backups created by this task could be used later by [create task](#task-instancecreate).

##### Work with remote instance backups

Backups can be automatically downloaded and uploaded from remote server.
 
Minimal requirement to have it working is only to specify `instance.backup.uploadUrl`.
By having only this upload property specified, plugin will automatically download most recent backup found in directory determined by upload URL.

It is also possible to specify second property `instance.backup.downloadUrl` which will cause that concrete backup will be always in use.
By having only this download property specified, plugin will not automatically upload any backups.

Most often only these few lines in *gradle.properties* files are required to have automatic two-way backups working:

```ini
localInstance.backup.uploadUrl=sftp://example.com/aem/packages
fileTransfer.sftp.user=foo
fileTransfer.sftp.password=pass
```

Protocols SFTP & SMB are supported by default.
However if there is a need to upload backups to cloud storage like Amazon S3, Google Cloud Storage it is possible by implementing custom file transfer.

```kotlin
aem {
    fileTransfer {
        custom("s3") {
            download { dirUrl: String, fileName: String, target: File ->
                // ...
            }
            upload { dirUrl: String, fileName: String, source: File ->
                // ...
            }
        }
    }
}
```

Custom file transfers could be used more widely than in only backup file resolution.
It is also possible to download packages to be satisfied on instances via custom file transfer or use it in task scripting:

```kotlin
aem {
    tasks {
        instanceSatisfy {
            packages {
                download("s3://packages/my-package.zip")
            }
        }
        tasks {
            register("doThings") {
                aem.fileTransfer.download("s3://packages/my-package.zip")
                aem.fileTransfer.upload("s3://packages", aem.`package`)
                // etc
            }
        }
    }
}
```

#### Task `instanceDestroy` 

Destroy local AEM instance(s).

To prevent data loss, this unsafe task execution must be confirmed by parameter `-Pforce`.
    
#### Task `instanceUp`

Turn on local AEM instance(s).

#### Task `instanceDown`

Turn off local AEM instance(s).

#### Task `instanceRestart`

Turn off and then turn on local AEM instance(s).

#### Task `instanceReload`

Reload OSGi Framework (Apache Felix) on local and remote AEM instance(s).

#### Task `instanceResolve`

Resolve instance files from remote sources before running other tasks.

Files considered:

* CRX packages configured in [satisfy task](#task-instancesatisfy)
* local instance source files (backup ZIP and AEM quickstart JAR & license file used by [create task](#task-instancecreate))

This task might be also useful to check amended configuration to verify HTTP urls, SMB / SSH credentials etc and fail fast when they are wrong.

#### Task `instanceSatisfy` 

Upload & install dependent CRX package(s) before deployment. Available methods:

* `group(name: String, options: Resolver<PackageGroup>.() -> Unit)`, useful for declaring group of packages (or just optionally naming single package) to be installed only on demand. For instance: `group("tools") { download('http://example.com/package.zip'); download('smb://internal-nt/package2.zip')  }`. Then to install only packages in group `tools`, use command: `gradlew instanceSatisfy -Pinstance.satisfy.group=tools`.
* `useLocal(path: String)`, use CRX package from local file system.
* `useLocal(file: File)`, same as above, but file can be even located outside the project.
* `resolve(notation: String)`, use OSGi bundle that will be resolved from defined Gradle repositories (for example from Maven) then wrapped to CRX package.
* `download(url: String)`, use CRX package that will be downloaded from specified URL to local temporary directory.
* `downloadHttp(url: String, options: HttpFileTransfer.() -> Unit)`, download package using HTTP with.
* `downloadSftp(url: String, options: SftpFileTransfer.() -> Unit)`, download package using SFTP protocol.
* `downloadSmb(url: String, options: SmbFileTransfer.() -> Unit)`, download package using SMB protocol.

Example configuration:

```kotlin
aem {
    tasks {
        instanceSatisfy {
            packages {
                group("default") {
                    useLocal("pkg/vanityurls-components-1.0.2.zip")
                    download("smb://company-share/aem/packages/my-lib.zip")
                    download("sftp://company-share/aem/packages/other-lib.zip")
                    download("file:///C:/Libraries/aem/package/extra-lib.zip")
                }

                group("tools") {
                    resolve("com.neva.felix:search-webconsole-plugin:1.2.0")
                    download("https://github.com/Cognifide/APM/releases/download/cqsm-3.0.0/apm-3.0.0.zip")
                    download("https://github.com/Adobe-Consulting-Services/acs-aem-tools/releases/download/acs-aem-tools-1.0.0/acs-aem-tools-content-1.0.0-min.zip")
                }
            }
        }
    }
}
```

By default, all packages will be deployed when running task `instanceSatisfy`.
Although, by grouping packages, there are available new options:

* group name could be used to filter out packages that will be deployed (`-Pinstance.satisfy.group=tools`, wildcards supported, comma delimited).
* after satisfying particular group, there are being run instance stability checks automatically (this behavior could be customized).

Task supports hooks for preparing (and finalizing) instance before (after) deploying packages in group on each instance. 
Also there is a hook called when satisfying each package group on all instances completed (for instance for awaiting stable instances which is a default behavior).
In other words, for instance, there is ability to run groovy console script before/after deploying some CRX package and then restarting instance(s) if it is exceptionally required.

```kotlin
aem {
    tasks {
        satisfy {
            packages {
                group("tool.groovy-console") { 
                    download("https://github.com/OlsonDigital/aem-groovy-console/releases/download/11.0.0/aem-groovy-console-11.0.0.zip")
                    config {
                        instanceName = "*-author" // additional filter intersecting 'instance.name' property
                        initializer {
                            logger.info("Installing Groovy Console on $instance")
                        }
                        finalizer {
                            logger.info("Installed Groovy Console on $instance")
                        }
                        completer {
                            logger.info("Reloading instance(s) after installing Groovy Console")
                            aem.instanceActions.reloadAndAwaitUp()
                        }
                    }
                }
            }
        }
    }
}
```

It is also possible to specify packages to be deployed only once via command line parameter, without a need to specify them in build script. Also for local files at any file system paths.

```bash
gradlew instanceSatisfy -Pinstance.satisfy.urls=[url1,url2]
```

For instance:

```bash
gradlew instanceSatisfy -Pinstance.satisfy.urls=[https://github.com/OlsonDigital/aem-groovy-console/releases/download/11.0.0/aem-groovy-console-11.0.0.zip,https://github.com/neva-dev/felix-search-webconsole-plugin/releases/download/search-webconsole-plugin-1.2.0/search-webconsole-plugin-1.2.0.jar]
```

#### Task `instanceCheck`

Check health condition of AEM instance(s) of any type (local & remote).

Customize behavior of each particular health check using following lambdas:

```kotlin
aem {
    tasks {
        instanceCheck {
            awaitUp {
                timeout {
                    stateTime = aem.props.long("instance.awaitUp.timeout.stateTime") ?: TimeUnit.MINUTES.toMillis(2)
                    constantTime = aem.props.long("instance.awaitUp.timeout.constantTime") ?: TimeUnit.MINUTES.toMillis(10)
                }
                bundles {
                    symbolicNamesIgnored = aem.props.list("instance.awaitUp.bundles.symbolicNamesIgnored") ?: listOf()
                }
                components {
                    platformComponents = aem.props.list("instance.awaitUp.components.platform") ?: listOf(
                        "com.day.crx.packaging.*", 
                        "org.apache.sling.installer.*"
                    )
                    specificComponents = aem.props.list("instance.awaitUp.components.specific") ?: aem.javaPackages.map { "$it.*" }
                }
                events {
                    unstableTopics = aem.props.list("instance.awaitUp.event.unstableTopics") ?: listOf(
                        "org/osgi/framework/ServiceEvent/*",
                        "org/osgi/framework/FrameworkEvent/*",
                        "org/osgi/framework/BundleEvent/*"
                    )
                    unstableAgeMillis = aem.props.long("instance.awaitUp.event.unstableAgeMillis") ?: TimeUnit.SECONDS.toMillis(5)
                }
            }
        }
    }
}
```

By default, `packageDeploy` task is also awaiting up instances (this could be optionally disabled by property `package.deploy.awaited=false`).
So it is also possible to configure each health check there:

```kotlin
aem {
    tasks {
        packageDeploy {
            awaitUp {
                // ...
            }
        }
    }
}
```

#### Task `instanceTail`

Continuosly downloads logs from any local or remote AEM instances.
Detects and interactively notifies about unknown errors as incident reports.

Tailer eliminates a need for connecting to remote environments using SSH protocol to be able to run `tail` command on that servers. 
Instead, tailer is continuously polling log files using HTTP endpoint provided by Sling Framework. 
New log entries are being dynamically appended to log files stored on local file system in a separate file for each environment. 
By having all log files in one place, AEM developer or QA engineer has an opportunity to comportably analyze logs, verify incidents occuring on AEM instances.

To customize tailer behavior, see [InstanceTailer](src/main/kotlin/com/cognifide/gradle/aem/instance/tail/InstanceTailer.kt).

```kotlin
aem {
    tasks {
        instanceTail {
            tailer {
                logFilePath = aem.props.string("instance.tail.logFilePath") ?: "/logs/error.log"
                logListener = { instance -> /* ... */ }
                incidentFilter = aem.props.string("instance.tail.incidentFilter")?.let { aem.project.file(it) } ?: File(aem.configCommonDir, "instanceTail/incidentFilter.txt")
                incidentDelay = aem.props.long("instance.tail.incidentDelay") ?: 5000L
            }
        }
    }
}
```

Log files are stored under directory: *build/aem/instanceTail/${instance.name}/error.log*.

##### Tailing incidents

By default, tailer is buffering cannonade of log entries of level *ERROR* in 5 seconds time window then interactively shows notification.
Clicking on that notification will browse to incident log file created containing only desired exceptions. These incident files are stored under directory: *build/aem/instanceTail/${instance.name}/incidents/${timestamp}-error.log*.

Which type of log entries are treated as a part of incident is determined by:

* property `-Pinstance.tail.incidentLevels=[ERROR,WARN]`
* wildcard exclusion rules defined in file which location is controlled by property `-Pinstance.tail.incidentFilter=aem/gradle/instanceTail/incidentFilter.txt`

Sample content of  *incidentFilter.txt* file, which holds a fragments of log entries that will be treated as known issues (notifications will be no longer shown):

```text
# On Unix OS, it is required to have execution rights on some scripts:
Error while executing script *diskusage.sh
Error while executing script *cpu.sh
```

##### Tailing multiple instances

Common use case could be to tail many remote AEM instances at once that comes from multiple environments.
To cover such case, it is possible to run tailer using predefined instances and defined dynamically. Number of specified instance URLs is unlimited.

Simply use command:

```bash
gradlew instanceTail -Pinstance.list=[http://admin:admin@192.168.1.1:4502,http://admin:admin@author.example.com]
```

##### Standalone tailer tool

Instance tailer could be used as standalone tool beside of e.g Maven based AEM application builds using [Content Package Maven Plugin](https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/vlt-mavenplugin.html).
Just download it from [here](dists/gradle-aem-tailer) (< 100 KB), extract anywhere on disk and run.

### Environment plugin

```kotlin
plugins {
    id("com.cognifide.aem.environment")
}
```

Controls virtualized AEM environment consisting of Apache Web Server (HTTPD) with AEM Dispatcher module installed.
Web Server is deployed as [Docker Container](https://www.docker.com/resources/what-container) orchestrated by [Swarm](https://docs.docker.com/engine/swarm/). This solution is cross-platform just like Docker.

Provides environment related tasks: `environmentUp`, `environmentDev`, `environmentHosts` etc.

Should be applied only at root project / only once within whole build.

Inherits from [Common Plugin](#common-plugin).

#### Environment configuration

Most of the configuration steps are automated. However, there are three manual steps to make this setup fully operating:

1. [Install Docker](https://docs.docker.com/install/)
    * [Mac](https://docs.docker.com/docker-for-mac/)
    * [Ubuntu](https://docs.docker.com/install/linux/docker-ce/ubuntu/)
    * Windows
        * [Desktop](https://docs.docker.com/docker-for-windows/install/) (highly recommended, using Hyper-V)
        * [Toolbox](https://docs.docker.com/toolbox/toolbox_install_windows/) (legacy, using VirtualBox)

2. Setup hosts on local machine (admin rights are required to access `/etc/hosts` or `C:\Windows\System32\drivers\etc\hosts` file)
    * In project based on [Gradle AEM Multi](https://github.com/Cognifide/gradle-aem-multi/blob/master/aem), just run script `aem/hosts` or `aem/hosts.bat`
    * Otherwise 
        * Unix: execute `sudo gradlew environmentHosts --no-daemon`
        * Windows: start PowerShell with "Run as administrator", then execute `./gradlew.bat environmentHosts --no-daemon`
    
##### Notice for Docker on Windows

Ensure having shared folders enabled via GUI

![Docker Desktop Shared Drive](docs/docker-desktop-share.png)

Because environment is using Docker volumes, on Windows, running task `environmentUp` will require additional user confirmation to allow virtualized container to access local configuration files.

##### Notice for Docker Toolbox

While using Docker Toolbox, beware that, by default, there is only one shared folder in VirtualBox configured which mounts *C:/Users* to */c/users* on Docker Machine / Ubuntu. 
As a consequence, if AEM project is located outside of *C:/Users* directory, there is a need to manually add corresponding shared folder and mount it on Docker Machine using VirtualBox GUI.

#### Environment file structure

Environment plugin is a little more depending on convention in case of directory structure.
Screenshot below presents recommended file structure which does not need any additional configuration.

![Environment file structure](docs/environment-file-structure.png)

Ensure having directory *aem/.environment* ignored in VCS and excluded from indexing by IDE.

Environment as a code paradigm is a main reason of locating environment configuration and runtime inside a project. 
Nowadays, advanced web applications (like AEM applications) are multi-layered and closely related with environment.
By having AEM dispatcher configuration and HTTDP configuration near application code base, the changes could be applied very easily and quickly.
By having all source code opened in single IDE window, e.g finding and amending content repository path occurrences at same time both in AEM application and HTTPD configuration could reduce risk of regression and they could be versioned once.
All code base - application and environment - could be treated as a whole, fully operating unit.

#### Environment service health checks

In case of the dispatcher it takes few seconds to start. Service health checks could be described by following configuration.
For example, to wait for all domains to be available and having particular text in response, write:

```kotlin
aem {
    environment {
        // ...

        healthChecks {
            url("Live site", "http://example.com/en-us.html", text = "English")
            url("Demo site", "http://demo.example.com/en-us.html", text = "English")
            url("Author login", "http://author.example.com/libs/granite/core/content/login.html" +
                    "?resource=%2F&\$\$login\$\$=%24%24login%24%24&j_reason=unknown&j_reason_code=unknown", text = "AEM Sign In")
        }
    }
}
```

#### Task `environmentUp`

Turns on local AEM environment.

![Environment up task](docs/environment-up-task.gif)

#### Task `environmentDown`

Turns off local AEM environment.

![Environment down task](docs/environment-down-task.gif)

#### Task `environmentDev`

Allows to listen for Apache Web Server / Dispatcher configuration files changed and then automatically reload HTTP service.

Workflow:

1. Run command `gradlew environmentDev`,
2. Edit files located in *aem/gradle/environment/httpd/conf* ,
3. Notice that HTTPD service should be restarted automatically after file changes,
4. Check results of [environment service health checks](#environment-service-health-checks),
4. Optionally, check:
   * HTTPD and AEM Dispatcher logs located at path *aem/.environment/httpd/logs*
   * AEM Dispatcher cache located at path *aem/.environment/httpd/cache*
   
![Environment up task](docs/environment-dev-task.gif)

## How to's

### Set AEM configuration properly for all / concrete project(s)

Common configuration like root of content for JCR package, should be defined in `allprojects` section like below / e.g in root *build.gradle.kts* file:

```kotlin
allprojects {
  plugins.withId("com.cognifide.aem.common") {
    configure<AemExtension> {
        `package` {
            contentDir = aem.project.file("src/main/aem") // overrides default dir named 'content'
        }
    }
  }
  
  plugins.withId("com.cognifide.aem.bundle") {
    configure<AemExtension> {
        tasks {
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
aem {
    tasks {
        bundle {
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

### Use lifecycle tasks

To control lifecycle of both:
 
* AEM instances (author & publish) (provided from [instance plugin](#instance-plugin))
* AEM environment (HTTPD server with AEM dispatcher) (provided by [environment plugin](#environment-plugin))

use dynamically registered tasks (only if at least one of plugins above are applied):

* `up` <=> `instanceUp` + `environmentUp`
* `down` <=> `instanceDown` + `environmentDown`
* `destroy` <=> `instanceDestroy` + `environmentDestroy`
* `setup` <=> `instanceSetup` + `environmentSetup`
* `resetup` <=> `instanceResetup` + `environmentResetup`
* `resolve` <=> `instanceResolve` + `environmentResolve`
 

### Implement custom AEM tasks

Most of built-in tasks logic is based on`aem` object of type [AemExtension](src/main/kotlin/com/cognifide/gradle/aem/common/AemExtension.kt). 
It provides concise AEM related API for accessing AEM configuration, synchronizing with AEM instances via specialized methods of `aem.sync` to make tasks implementation a breeze.
What is more, it also provides built-in HTTP client `aem.http` to be able to communicate with any external services like for downloading CRX packages from package shares like Nexus repositories, JFrog Artifactory etc.
The options are almost unlimited. 

#### Defining CRX package via code then downloading and sharing it using external HTTP endpoint

Below snippet could be used to automatize creation of production content backups.

```kotlin
aem {
    tasks {
        register("backupProductionAuthor") {
            doLast {
                val pkg = aem.namedInstance("prod-author").sync {
                    downloadPackage {
                        group = "example"
                        name = "backup"
                        description = "Backup of content, tags and DAM"
                        archiveName = "backup-author.zip"
                        filters(
                                "/content/cq:tags/example",
                                "/content/example",
                                "/content/dam/example"
                        )
                    }
                }
    
                aem.http {
                    basicUser = "foo"
                    basicPassword = "bar"
                    postMultipart("http://my-aem-backup-service.com/package/upload", mapOf("file" to pkg)) 
                }
            }
        }
    }
}
```

#### Calling AEM endpoints / making any HTTP requests

To make an HTTP request to some AEM endpoint (servlet) simply write:

```kotlin
aem {
    tasks {
        register("runHealthCheck") {
            doLast {
                aem.sync {
                    http {
                        get("/bin/example/healthCheck") { checkStatus(it, 200) }
                    }
                }
            }
        }
    }
}
```

There are unspecified AEM instances as `aem.sync` method parameter so that instances matching [default filtering](#filter-instances-to-work-with) will be used.

The fragment `{ checkStatus(it, 200) }` could be even ommitted because, by default sync API checks status code that it belongs to range [200,300\).

To parse endpoint response as [JSON](http://static.javadoc.io/com.jayway.jsonpath/json-path/2.4.0/com/jayway/jsonpath/DocumentContext.html) (using [JsonPath](https://github.com/json-path/JsonPath)), simply write:

```kotlin
aem {
    tasks {
        register("runHealthCheck") {
            doLast {
                aem.sync {
                    http {
                        val json = get("/bin/example/healthCheck") { asJson(it) }
                        val status = json.read("status") as String
                        
                        if (status != "OK") {
                            throw GradleException("Health check failed on: $instance because status '$status' detected.")
                        }
                    }
                }
            }
        }
    }
}
```

There are also available convenient methods `asStream`, `asString` to be able to process endpoint responses.

#### Downloading CRX package from external HTTP endpoint and deploying it on desired AEM instances

Below snippet could be used to automatize recovery from content backups (e.g for production or to replicate production content to test environment).

```kotlin

aem {
    tasks {
        register("deployProductionContent") {
            doLast {
                val instances = listOf(
                        aem.instance("http://user:password@aem-host.com") // URL specified directly, could be parametrized by some gradle command line property
                        // aem.namedInstance("local-publish") // reused AEM instance defined in 'gradle.properties'
                )
                val pkg = aem.httpFile { download("https://company.com/aem/backups/example-1.0.0-201901300932.backup.zip") }
                
                aem.sync(instances) { 
                    packageManager.deploy(pkg) 
                }
            }
        }
    }
}
```

#### Working with content repository (JCR)

To make changes in AEM content repository, use [Repository](src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/repository/Repository.kt) instance service which is a part of instance sync tool.

For example, to migrate pages even without using [Groovy Console](https://github.com/icfnext/aem-groovy-console) deployed on instance, simply write:

```kotlin
aem {
    tasks {
        register("migratePages") {
            description = "Migrates pages to new component"
            doLast {
                aem.sync {
                    repository {
                        node("/content/example")
                            .traverse()
                            .filter { it.type == "cq:PageContent" && properties["sling:resourceType"] == "example/components/basicPage" }
                            .forEach { page ->
                                logger.info("Migrating page: ${page.path}")
                                page.saveProperty("sling:resourceType", "example/components/advancedPage")
                            }
                    }
                }
            }
        }
    }
}
```

To create new / update existing nodes to configure e.g replication agents write:

```kotlin
aem {
    tasks {
        register("setupReplicationAgents") {
            description = "Corrects publish replication agent transport URI"
            doLast {
                aem.sync {
                    repository {
                        node("/etc/replication/agents.publish/flush/jcr:content", mapOf( // shorthand for 'node(path).save(props)'
                            "transportUri" to "http://invalidation-only/dispatcher/invalidate.cache"
                        ))
                    }
                }
            }
        }
    }
}
```

Under the hood, repository service is using only AEM built-in [Sling Post Servlet](https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html).

#### Executing code on AEM runtime

It is also possible to easily execute any code on AEM runtime using [Groovy Console](https://github.com/icfnext/aem-groovy-console). 
Assuming that on AEM instances there is already installed Groovy Console e.g via `instanceSatisfy` task, then it is possible to use [GroovyConsole](src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/groovy/GroovyConsole.kt) instance service.

```kotlin
aem {
    tasks {
        satisfy {
            group("tool.groovyconsole") { download("https://github.com/icfnext/aem-groovy-console/releases/download/12.0.0/aem-groovy-console-12.0.0.zip") }
        }
        register("generatePosts") {
            doLast {
                aem.sync {
                    groovyConsole.evalCode("""
                        def postsService = getService("com.company.example.aem.sites.services.posts.PostsService")
                        
                        println postsService.randomPosts(5)
                    """)
                    // groovyConsole.evalScript("posts.groovy") // if script above moved to 'aem/gradle/groovyScript/posts.groovy'
                }
            }
        }
    }
}
```

#### Controlling OSGi bundles and components

To disable specific OSGi component by its PID value and only on publish instances use [OsgiFramework](src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/osgi/OsgiFramework.kt) instance service and write:


```kotlin
aem {
    tasks {
        register("instanceSecure") {
            doLast {
                aem.sync(aem.publishInstances) {
                    osgiFramework.disableComponent("org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet")
                    // osgiFramework.stopBundle("org.apache.sling.jcr.webdav")
                }
            }
        }
    }
}
```

### Understand why there are one or two plugins to be applied in build script

Gradle AEM Plugin assumes separation of 5 plugins to properly fit into Gradle tasks structure correctly.

Most often, Gradle commands are being launched from project root and tasks are being run by their name e.g `instanceSatisfy` (which is not fully qualified, better if it will be `:instanceSatisfy` of root project).
Let's imagine if task `instanceSatisfy` will come from package plugin, then Gradle will execute more than one `instanceSatisfy` (for all projects that have plugin applied), so that this is unintended behavior.
Currently used plugin architecture solves that problem.

### Work effectively on start and daily basis

Initially, to create fully configured local AEM instances simply run command `gradlew instanceSetup`.

Later during development process, building and deploying to AEM should be done using the simplest command: `gradlew`.
Above configuration uses [default tasks](https://docs.gradle.org/current/userguide/tutorial_using_tasks.html#sec:default_tasks), so that alternatively it is possible to do the same using explicitly specified command `gradlew instanceSatisfy packageDeploy instanceCheck`.

* Firstly dependent packages (like AEM hotfixes, Vanity URL Components etc) will be installed lazily (only when they are not installed yet).
* In next step application is being built and deployed to all configured AEM instances.
* Finally build awaits till all AEM instances are stable.

### Filter instances to work with

When there are defined named AEM instances: `local-author`, `local-publish`, `integration-author` and `integration-publish`,
then it is possible to:

* deploy (or satisfy) CRX package(s)
* tail logs
* checkout JCR content 

with taking into account: 

 * type of environment (local, integration, staging, etc)
 * type of AEM instance (author / publish)

by filtering instances by names, e.g:

```bash
gradlew packageDeploy -Pinstance.name=integration-*
gradlew packageDeploy -Pinstance.name=*-author
gradlew packageDeploy -Pinstance.name=local-author,integration-author
```

Default value of that instance name filter is `${environment}-*`, so that typically `local-*`.
Environment value comes from system environment variable `ENV` or property `env`.

To deploy only to author or publish instances:

```bash
gradlew packageDeploy -Pinstance.authors
gradlew packageDeploy -Pinstance.publishers
```

To deploy only to instances specified explicitly:

```bash
gradlew packageDeploy -Pinstance.list=[http://admin:admin@localhost:4502,http://admin:admin@localhost:4503]
```

Instance urls must be delimited by colon. 
Remember to [encode instance user & password](#instance-url-credentials-encoding) properly.

### Know how properties are being expanded in instance or package files

The properties syntax comes from [Pebble Template Engine](https://github.com/PebbleTemplates/pebble) which means that all its features (if statements, for loops, filters etc) can be used inside files being expanded.

Expanding properties could be used separately on any string or file source in any custom task by using method `aem.props.expand()`.

## Known issues

### No OSGi services / components are registered

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

### Caching task `packageCompose`

Expandable properties with dynamically calculated value (unique per build) like `created` and `buildCount` are not used by default generated properties file intentionally, 
because such usages will effectively forbid caching `packageCompose` task and it will be never `UP-TO-DATE`.

### Vault tasks parallelism

Vault tool current working directory cannot be easily configured, because of its API. AEM plugin is temporarily changing current working directory for Vault, then returning it back to original value.
In case of that workaround, Vault tasks should not be run in parallel (by separated daemon processed / JVM synchronization bypassed), because of potential unpredictable behavior.

## Compatibility

| Gradle AEM Plugin | Gradle Build Tool | Adobe Experience Manager |
|:-----------------:|:-----------------:|:------------------------:|
|   4.x.x -> 5.x.x  |     4.x -> 4.8    |        6.x and up        |
|   6.0.0 -> 6.2.1  |     4.9 -> 5.0    |        6.x and up        |
|   6.3.0 and up    |     5.1 and up    |        6.x and up        |

## Building

1. Clone this project using command `git clone https://github.com/Cognifide/gradle-aem-plugin.git`
2. To build plugin, simply enter cloned directory run command: `gradlew`
3. To debug built plugin:
    * Append to build command parameters `--no-daemon -Dorg.gradle.debug=true`
    * Run build, it will suspend, then connect remote at port 5005 by using IDE
    * Build will proceed and stop at previously set up breakpoint.

## Contributing

Issues reported or pull requests created will be very appreciated. 

1. Fork plugin source code using a dedicated GitHub button.
2. Do code changes on a feature branch created from *develop* branch.
3. Create a pull request with a base of *develop* branch.

## License

**Gradle AEM Plugin** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)
