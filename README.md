![Cognifide logo](docs/cognifide-logo.png)

[![Gradle Status](https://gradleupdate.appspot.com/Cognifide/gradle-aem-plugin/status.svg?random=123)](https://gradleupdate.appspot.com/Cognifide/gradle-aem-plugin/status)
[![Apache License, Version 2.0, January 2004](docs/apache-license-badge.svg)](http://www.apache.org/licenses/)
![Travis Build](https://travis-ci.org/Cognifide/gradle-aem-plugin.svg?branch=develop)

# Gradle AEM Plugin

<p align="center">
  <img src="docs/logo.png" alt="Gradle AEM Plugin Logo"/>
</p>

## Description

Swiss army knife for AEM related automation. Incremental build which takes seconds, not minutes. Developer who does not loose focus between build time gaps. Extend freely your build system directly in project. 

AEM developer - it's time to meet Gradle! You liked or used plugin? Don't forget to **star this project** on GitHub :)

Looking for dedicated version of plugin for [**Apache Sling**](https://sling.apache.org)? Check out [Gradle Sling Plugin](https://github.com/Cognifide/gradle-sling-plugin)!

### Screenshot

<p align="center">
  <img src="docs/example-build.gif" alt="Example Project Build"/>
</p>

### Features

* Composing CRX package from multiple JCR content roots, bundles.
* Fully automated, native (no virtulization), tied to project, AEM instance(s) setup allowing to start development within few minutes.
* Powerful AEM DSL scripting capabilities for performing content migrations, managing instances.
* Advanced AEM instance(s) stability & health checking after CRX package deployment.
* Automated all-in-one CRX packages generation (assemblies), vault filters merging etc.
* Easy parallel CRX package deployment to many remote group of instances.
* Automated dependent CRX packages and OSGi bundles installation from local and remote sources (SMB, SSH, HTTP(s)).
* Smart Vault files generation (combining defaults with overiddables).
* Embedded [Vault Tool](http://jackrabbit.apache.org/filevault) for checking out and cleaning JCR content from running AEM instance.
* Embedded [BND Tool](https://github.com/bndtools/bnd/tree/master/biz.aQute.bnd.gradle) for OSGi Manifest customization.

## Important notice 

Gradle AEM Plugin 6.x serie and upper will **no longer support Groovy DSL** and **stands on Kotlin DSL** coming with Gradle 5.0.

To see documentation for previous 5.x serie, please [click here](https://github.com/Cognifide/gradle-aem-plugin/tree/groovy) (navigate to branch `groovy`).

## Table of contents

  * [Getting started](#getting-started)
  * [Configuration](#configuration)
     * [Plugin setup](#plugin-setup)
        * [Minimal:](#minimal)
        * [Additional](#additional)
     * [Config plugin](#config-plugin)
        * [Instance conventions](#instance-conventions)
        * [Defining instances via properties file](#defining-instances-via-properties-file)
        * [Defining instances via build script](#defining-instances-via-build-script)
     * [Tooling plugin](#tooling-plugin)
        * [Task aemSync](#task-aemsync)
           * [Cleaning features](#cleaning-features)
           * [Default cleaning configuration](#default-cleaning-configuration)
           * [Rendition cleaning configuration](#rendition-cleaning-configuration)
           * [Using alternative transfer type](#using-alternative-transfer-type)
           * [Copying or cleaning content only](#copying-or-cleaning-content-only)
           * [Filter file at custom path](#filter-file-at-custom-path)
           * [Filter roots specified explicitly](#filter-roots-specified-explicitly)
        * [Task aemRcp](#task-aemrcp)
        * [Task aemVlt](#task-aemvlt)
        * [Task aemDebug](#task-aemdebug)
     * [Package plugin](#package-plugin)
        * [Task aemCompose](#task-aemcompose)
           * [Default configuration](#default-configuration)
        * [Including additional OSGi bundle into CRX package](#including-additional-osgi-bundle-into-crx-package)
           * [Assembling packages (merging into all-in-one)](#assembling-packages-merging-into-all-in-one)
           * [Expandable properties](#expandable-properties)
        * [Task aemDeploy](#task-aemdeploy)
           * [Deploying only to author or publish instances](#deploying-only-to-author-or-publish-instances)
           * [Deploying only to instances specified explicitly](#deploying-only-to-instances-specified-explicitly)
        * [Task aemUpload](#task-aemupload)
        * [Task aemDelete](#task-aemdelete)
        * [Task aemInstall](#task-aeminstall)
        * [Task aemUninstall](#task-aemuninstall)
        * [Task aemPurge](#task-aempurge)
        * [Task aemActivate](#task-aemactivate)
     * [Bundle plugin](#bundle-plugin)
        * [Bundle conventions](#bundle-conventions)
        * [Embedding JAR file into built OSGi bundle](#embedding-jar-file-into-built-osgi-bundle)
        * [Configuring OSGi bundle manifest attributes](#configuring-osgi-bundle-manifest-attributes)
        * [Excluding packages being incidentally imported by OSGi bundle](#excluding-packages-being-incidentally-imported-by-osgi-bundle)
     * [Instance plugin](#instance-plugin)
        * [Task aemSetup](#task-aemsetup)
        * [Task aemResetup](#task-aemresetup)
        * [Task aemCreate](#task-aemcreate)
           * [Source files configuration](#source-files-configuration)
           * [Extracted files configuration (optional)](#extracted-files-configuration-optional)
        * [Task aemBackup](#task-aembackup)
        * [Task aemDestroy](#task-aemdestroy)
        * [Task aemUp](#task-aemup)
        * [Task aemDown](#task-aemdown)
        * [Task aemRestart](#task-aemrestart)
        * [Task aemReload](#task-aemreload)
        * [Task aemSatisfy](#task-aemsatisfy)
        * [Task aemAwait](#task-aemawait)
        * [Task aemCollect](#task-aemcollect)
  * [How to's](#how-tos)
     * [Set AEM configuration properly for all / concrete project(s)](#set-aem-configuration-properly-for-all--concrete-projects)
     * [Understand why there are one or two plugins to be applied in build script](#understand-why-there-are-one-or-two-plugins-to-be-applied-in-build-script)
     * [Work effectively on start and daily basis](#work-effectively-on-start-and-daily-basis)
     * [Filter instances for which packages will be deployed or satisfied](#filter-instances-for-which-packages-will-be-deployed-or-satisfied)
     * [Know how properties are being expanded in instance or package files](#know-how-properties-are-being-expanded-in-instance-or-package-files)
  * [Known issues](#known-issues)
     * [No OSGi services / components are registered](#no-osgi-services--components-are-registered)
     * [Caching task aemCompose](#caching-task-aemcompose)
     * [Vault tasks parallelism](#vault-tasks-parallelism)
     * [Files from SSH for aemCreate and <code>aemSatisfy</code>](#files-from-ssh-for-aemcreate-and-aemsatisfy)
  * [Building](#building)
  * [Contributing](#contributing)
  * [License](#license)

## Getting started

* Most effective way to experience Gradle AEM Plugin is to use *Quickstart* located in:
  * [AEM Single-Project Example](https://github.com/Cognifide/gradle-aem-single#quickstart) - recommended for **application** development,
  * [AEM Multi-Project Example](https://github.com/Cognifide/gradle-aem-multi#quickstart) - recommended for **project** development,
* The only needed software to start using plugin is to have installed on machine Java 8.
* As a build command, it is recommended to use Gradle Wrapper (`gradlew`) instead of locally installed Gradle (`gradle`) to easily have same version of build tool installed on all environments. Only at first build time, wrapper will be automatically downloaded and installed, then reused.

## Configuration

### Plugin setup

Released versions of plugin are available on [Bintray](https://bintray.com/cognifide/maven-public/gradle-aem-plugin), 
so that this repository need to be included in *buildscript* section.

#### Minimal:

Configuration below assumes building and deploying CRX packages to AEM instance(s) via command: `gradlew aemDeploy`.

File *buildSrc/build.gradle.kts*:

```kotlin
repositories {
    jcenter()
}

dependencies {
    implementation("com.cognifide.gradle:aem-plugin:6.0.0")
}
```

File *build.gradle.kts*:

```kotlin
plugins {
    id("com.cognifide.aem.bundle") // or 'package' for JCR content only
}
```

#### Additional

Configuration below assumes building and deploying on AEM instance(s) via command: `gradlew` (default tasks will be used).

```kotlin
plugins {
    id("com.cognifide.aem.bundle")
    id("com.cognifide.aem.instance")
    id("org.jetbrains.kotlin.jvm") // or any other like 'java' to compile OSGi bundle
}

defaultTasks = listOf(":aemSatisfy", ":aemDeploy")

aem {
    config {
        packageRoot = "${aem.project.file("src/main/content")}"
        // ...
               
    }
    tasks {
        compose {
            fromProject(':core')
            fromProject(':config')
        }
        bundle {
            javaPackage = "com.company.example.aem"
            // ...
        }
        satisfy {
            url('http://.../package.zip')
        }
    }
}
```

To see all available options and actual documentation, please follow to:

* `aem` - [AemExtension](src/main/kotlin/com/cognifide/gradle/aem/common/AemExtension.kt)
* `config` - [Config](src/main/kotlin/com/cognifide/gradle/aem/config/Config.kt)
* `compose` - [Compose](src/main/kotlin/com/cognifide/gradle/aem/pkg/tasks/Compose.kt)
* `bundle` - [BundleJar](src/main/kotlin/com/cognifide/gradle/aem/bundle/BundleJar.kt)
* `satisfy` - [Satisfy](src/main/kotlin/com/cognifide/gradle/aem/instance/tasks/Satisfy.kt)
* `...` - other tasks in similar way.

### Config plugin

```kotlin
plugins {
    id("com.cognifide.aem.config")
}
```

Applied transparently by other plugins. Provides AEM section to build script and instance definitions, common configuration.

It does not provide any tasks.

#### Instance conventions

* Instance **name** is a combination of *${environment}-${typeName}* e.g *local-author*, *integration-publish* etc.
* Instance **type** indicates physical type of instance and could be only: *local* and *remote*. Local means that instance could be created by plugin automatically under local file system.
* Instance **type name** is an instance purpose identifier and must start with prefix *author* or *publish*. Sample valid names: *author*, *author1*, *author2*, *author-master* and *publish*, *publish1* *publish2* etc.
* Only instances defined as *local* are considered in command `aemSetup`, `aemCreate`, `aemUp` etc (that comes from `com.cognifide.aem.instance` plugin).
* All instances defined as *local* or *remote* are considered in commands CRX package deployment related like `aemSatisfy`, `aemDeploy`, `aemUpload`, `aemInstall` etc.

Instances could be defined in two ways, via:
 
* file `gradle.properties` - recommended approach, by properties convention.
* build script - customizable approach.

#### Defining instances via properties file

The configuration could be specified through *gradle.properties* file using dedicated syntax.

`aem.instance.$ENVIRONMENT-$TYPE_NAME.$PROP_NAME=$PROP_VALUE`

Part | Possible values | Description |
--- | --- | --- |
`$ENVIRONMENT` | `local`, `int`, `stg` etc | Environment name. |
`$TYPE_NAME` | `author`, `publish`, `publish2`, etc | Combination of AEM instance type and semantic suffix useful when more than one of instance of same type is being configured. |
`$PROP_NAME=$PROP_VALUE` | **Local instances:** `httpUrl=http://admin:admin@localhost:4502`<br>`type=local`(or remote)<br>`password=foo`<br>`runModes=nosamplecontent`<br>`jvmOpts=-server -Xmx2048m -XX:MaxPermSize=512M -Djava.awt.headless=true`, `startOpts=...`<br>`debugPort=24502`.<br><br>**Remote instances:** `httpUrl`, `type`, `user`, `password`. | Run modes, JVM opts and start opts should be comma delimited. |


Default remote instances defined via properties (below lines are optional):

```
aem.instance.local-author.httpUrl=http://localhost:4502
aem.instance.local-publish.httpUrl=http://localhost:4503
```

Example for defining multiple remote instances:

```
aem.instance.int-author.httpUrl=http://author.aem-integration.company.com
aem.instance.int-publish.httpUrl=http://aem-integration.company.com
aem.instance.stg-author.httpUrl=http://author.aem-staging.company.com
aem.instance.stg-publish.httpUrl=http://aem-staging.company.com
```

Example for defining remote instance with credentials separated:

```
aem.instance.test-author.httpUrl=http://author.aem-integration.company.com
aem.instance.test-author.user=foo
aem.instance.test-author.password=bar
```

Example for defining remote instance with credentials details included in URL:

```
aem.instance.test-author.httpUrl=http://foo:bar@author.aem-integration.company.com
```

Example for defining local instances (created on local file system):

```
aem.instance.local-author.httpUrl=http://localhost:4502
aem.instance.local-author.type=local
aem.instance.local-author.runModes=nosamplecontent
aem.instance.local-author.jvmOpts=-server -Xmx1024m -XX:MaxPermSize=256M -Djava.awt.headless=true

aem.instance.local-publish.httpUrl=http://localhost:4503
aem.instance.local-publish.type=local
aem.instance.local-publish.runModes=nosamplecontent
aem.instance.local-publish.jvmOpts=-server -Xmx1024m -XX:MaxPermSize=256M -Djava.awt.headless=true
```

Notice! Remember to define also AEM [source files](#source-files-configuration).

#### Defining instances via build script

Example usage below. The commented value is an effective instance name.

```kotlin
aem {
    config {
        localInstance("http://localhost:4502") // local-author
        localInstance("http://localhost:4502") { // local-author
            user = "admin"
            password = "admin"
            typeName = "author"
            debugPort = 14502 
        }
      
        localInstance("http://localhost:4503") // local-publish
        localInstance("http://localhost:4503") { // local-publish
            user = "admin"
            password = "admin"
            typeName = "publish"
            debugPort = 14503
        } 
      
        remoteInstance("http://192.168.10.1:4502") { // integration-author1
            user = "user1" 
            password = "password2"
            environment = "integration"
            typeName = "author1"
        } 
        remoteInstance("http://192.168.10.1:8080") { // integration-author2
            user = "user1" 
            password = "password2"
            environment = "integration"
            typeName = "author2"
        } 
        remoteInstance("http://192.168.10.2:4503") { // integration-publish1
            user = "user2"
            password = "password2"
            environment = "integration"
            typeName = "publish1"
        } 
        remoteInstance("http://192.168.10.2:8080") { // integration-publish2
            user = "user2"
            password = "password2"
            environment = "integration"
            typeName = "publish2"
        } 
    }
}
```

### Tooling plugin

```kotlin
plugins {
    id("com.cognifide.aem.tooling")
}
```

Applied transparently by package and bundle plugins. Provides tooling related tasks like `aemSync`, `aemRcp`, `aemDebug` etc.

#### Task `aemSync`

Check out then clean JCR content. 

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
                    include(listOf("**/.vlt", "**/.vlt*.tmp")) 
                }
                filesFlattened = { 
                    include(listOf("**/_cq_dialog/.content.xml", "**/_cq_htmlTag/.content.xml")) 
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
        clean {
            options {
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
                    include(listOf(
                        "**/.vlt",
                         "**/.vlt*.tmp",
                        "**/content/dam/**/_jcr_content/folderThumbnail*",
                        "**/content/dam/**/_jcr_content/renditions/*"
                    ))
                }
            }  
        }
    }
}
```

##### Using alternative transfer type

Available transfer types: *package_download* (default) and *vlt_checkout*.

```bash
gradlew :content:aemSync -Paem.sync.type=vlt_checkout
```

##### Copying or cleaning content only

Available mode types: *copy_and_clean* (default), *clean_only* and *copy_only*.

```bash
gradlew :content:aemSync -Paem.sync.mode=clean_only
```

##### Filter file at custom path
   
```bash
gradlew :content:aemSync -Paem.filter.path=custom-filter.xml
gradlew :content:aemSync -Paem.filter.path=src/main/content/META-INF/vault/custom-filter.xml
gradlew :content:aemSync -Paem.filter.path=C:/aem/custom-filter.xml
```

##### Filter roots specified explicitly
   
```bash
gradlew :content:aemSync -Paem.filter.roots=[/etc/tags/example,/content/dam/example]
```

#### Task `aemRcp`

Copy JCR content from one instance to another. Sample usages below.

* Using predefined instances with multiple different source and target nodes:

  ```bash
  gradlew :aemRcp -Paem.rcp.source.instance=int-author -Paem.rcp.target.instance=local-author -Paem.rcp.paths=[/content/example-demo=/content/example,/content/dam/example-demo=/content/dam/example]
  ```

* Using predefined instances with multiple same source and target nodes:

  ```bash
  gradlew :aemRcp -Paem.rcp.source.instance=stg-author -Paem.rcp.target.instance=int-author -Paem.rcp.paths=[/content/example,/content/example2]
  ```

* Using predefined instances with source and target nodes specified in file:

  ```bash
  gradlew :aemRcp -Paem.rcp.source.instance=int-author -Paem.rcp.target.instance=local-author -Paem.rcp.pathsFile=paths.txt
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
  gradlew :aemRcp -Paem.rcp.source.instance=http://user:pass@192.168.66.66:4502 -Paem.rcp.target.instance=http://user:pass@192.168.33.33:4502 -Paem.rcp.paths=[/content/example-demo=/content/example]
  ```

Keep in mind, that copying JCR content between instances, could be a trigger for running AEM workflows like *DAM Update Asset* which could cause heavy load on instance.
Consider disabling AEM workflow launchers before running this task and re-enabling after.

RCP task is internally using [Vault Remote Copy](http://jackrabbit.apache.org/filevault/rcp.html) which requires to having bundle *Apache Sling Simple WebDAV Access to repositories (org.apache.sling.jcr.webdav)* " in active state on instance.

#### Task `aemVlt`

Execute any JCR File Vault command. 

For instance, to reflect `aemRcp` functionality, command below could be executed:

```bash
gradlew :content:aemVlt -Paem.vlt.command='rcp -b 100 -r -u -n http://admin:admin@localhost:4502/crx/-/jcr:root/content/dam/example http://admin:admin@localhost:4503/crx/-/jcr:root/content/dam/example' 
```

For more details about available parameters, please visit [VLT Tool documentation](https://docs.adobe.com/docs/en/aem/6-2/develop/dev-tools/ht-vlttool.html).

While using task `aemVlt` be aware that Gradle requires to have working directory with file *build.gradle.kts* in it, but Vault tool can work at any directory under *jcr_root*. To change working directory for Vault, use property `aem.vlt.path` which is relative path to be appended to *jcr_root* for project task being currently executed.

#### Task `aemDebug` 

Dumps effective AEM build configuration of concrete project to JSON file.

When command below is being run (for root project `:`):

```bash
gradlew :aemDebug
```

Then file at path *build/aem/aemDebug/debug.json* with content below is being generated:

```json
{
  "buildInfo" : {
    "plugin" : {
      "pluginVersion" : "6.0.0",
      "gradleVersion" : "5.0"
    },
    "gradle" : {
      "version" : "5.0",
      "homeDir" : ".../.gradle/wrapper/dists/gradle-5.0-all/.../gradle-5.0"
    },
    "java" : {
      "version" : "1.8",
      "homeDir" : ".../Java/jdk1.8.0_121/jre"
    }
  },
  "projectInfo" : {
    "displayName" : "project ':aem:app.core'",
    "path" : ":aem:app.core",
    "name" : "app.core",
    "dir" : ".../gradle-aem-multi/aem/app.core"
  },
  "baseConfig" : {
    "instances" : {
      "local-author" : {
        "properties" : { },
        "httpUrl" : "http://localhost:4502",
        "user" : "admin",
        "password" : "admin",
        "typeName" : "author",
        "environment" : "local",
        "name" : "local-author",
        "type" : "AUTHOR",
        "httpPort" : 4502
      }
    },
    "instanceRoot" : ".../.aem/example",
    "packageSnapshots" : [ ],
    "packageRoot" : ".../gradle-aem-multi/aem/app.core/src/main/content",
    "packageMetaCommonRoot" : ".../gradle-aem-multi/aem/gradle/META-INF",
    "packageInstallPath" : "/apps/example/app.core/install",
    "packageInstallRepository" : true,
    "packageErrors" : [
      "javax.jcr.nodetype.*Exception",
      "org.apache.jackrabbit.oak.api.*Exception",
      "org.apache.jackrabbit.vault.packaging.*Exception",
      "org.xml.sax.*Exception"
    ],
    "packageResponseBuffer" : 4096,
    "lineSeparator" : "LF",
    "notificationEnabled" : true,
    "groovyScriptRoot" : ".../gradle-aem-multi/aem/gradle/groovyScript"
  },
  "bundleConfig" : {
    "jar" : {
      "installPath" : "/apps/example/app.core/install",
      "attributesConvention" : true,
      "javaPackage" : "com.company.example.aem.app.core",
      "javaPackageOptions" : "-split-package:=merge-first",
      "bndPath" : ".../gradle-aem-multi/aem/app.core/bnd.bnd",
      "bndInstructions" : {
        "-fixupmessages.bundleActivator" : "Bundle-Activator * is being imported *;is:=error"
      },
      "attributes" : {
        "Manifest-Version" : "1.0",
        "Bundle-Category" : "example",
        "Bundle-Vendor" : "Company",
        "Bundle-Name" : "Example - AEM Application Core",
        "Bundle-SymbolicName" : "com.company.example.aem.app.core",
        "Sling-Model-Packages" : "com.company.example.aem.app.core",
        "Import-Package" : "*",
        "Export-Package" : "com.company.example.aem.app.core.*;-split-package:=merge-first"
      }
    }
  },
  "packageDeployed" : {
    "local-author" : null
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

Provides CRX package related tasks: `aemCompose`, `aemDeploy`, `aemActivate`, `aemPurge` etc.

Inherits from [Tooling Plugin](#tooling-plugin).

#### Task `aemCompose`

[Compose](src/main/kotlin/com/cognifide/gradle/aem/pkg/tasks/Compose.kt) CRX package from JCR content and bundles. 

Inherits from task [ZIP](https://docs.gradle.org/3.5/dsl/org.gradle.api.tasks.bundling.Zip.html).

##### Default configuration

```kotlin
aem {
    tasks {
        compose {
            duplicatesStrategy = DuplicatesStrategy.WARN
            baseName = aem.baseName
            contentPath = aem.config.packageRoot
            bundlePath = aem.config.packageInstallPath
            metaDefaults = true
            vaultProperties = mapOf(
                "acHandling" to "merge_preserve",
                "requiresRoot" to false
            )
            vaultName = baseName
            vaultGroup = project.group
            vaultVersion = project.version
            fromConvention = true
        }
    }    
}
```

#### Including additional OSGi bundle into CRX package

Use dedicated task method named `fromJar`.

```kotlin
aem {
    tasks {
        compose {
            fromJar("group:name:version")
        }
    }
}
```

For the reference, see [usage in AEM Multi-Project Example](https://github.com/Cognifide/gradle-aem-multi/blob/master/aem/app.common/build.gradle.kts).

##### Assembling packages (merging into all-in-one)

Let's assume following project structure (with build file contents):

* *build.gradle.kts* (project `:`)

```kotlin
plugins {
    id("com.cognifide.aem.package")
}

aem {
    tasks {
        compose {
            fromProjects(":app:*")
            fromProjects(":content:*")
            fromProject(":migration")
        }
    }    
}
```

When building via command `gradlew :build`, then the effect will be a CRX package with assembled JCR content and OSGi bundles from projects: `:app:core`, `:app:common`, `:content:init`, `:content:demo` and `:migration`.

* *app/build.gradle.kts*  (project `:app`)

```kotlin
plugins {
    id("com.cognifide.aem.package")
}

aem {
    tasks {
        compose {
            fromSubprojects()
        }
    }
}
```

When building via command `gradlew :app:build`, then the effect will be a CRX package with assembled JCR content and OSGi bundles from projects: `:app:core`, `:app:common` only.

* *app/core/build.gradle.kts* (project `:app:core`, JCR content and OSGi bundle)
* *app/common/build.gradle.kts* (project `:app:common`, JCR content and OSGi bundle)
* *content/init/build.gradle.kts* (project `:content:init`, JCR content only)
* *content/demo/build.gradle.kts* (project `:content:demo`, JCR content only)
* *migration/build.gradle.kts* (project `:migration`, JCR content only)
* *test/integration/build.gradle.kts* (project `:test:integration`, any source code)
* *test/functional/build.gradle.kts* (project `:test:functional`, any source code)

Gradle AEM Plugin is configured in that way that project can have:
 
* JCR content,
* source code to compile OSGi bundle,
* both.

By distinguishing `fromProject`, `fromBundle` or `fromCompose` there is ability to create any assembly CRX package with content of any type without restructuring the project.

When using `fromProject` there is an ability to pass lambda to customize options like `bundlePath`, `bundleRunMode`, decide to include only JCR contents, only bundles and more.

Only one must have rule to be kept while developing a multi-module project is that **all Vault filter roots of all projects must be exclusive**. In general, they are most often exclusive, to avoid strange JCR installer behaviors, but sometimes exceptional [workspace filter](http://jackrabbit.apache.org/filevault/filter.html) rules are being applied like `mode="merge"` etc.

##### Expandable properties

In exactly same way as it is working for instance files, properties can be expanded inside metadata files of package being composed.

Related configuration:

```kotlin
aem {
    tasks {
        compose {
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

* `compose` - [Compose](src/main/kotlin/com/cognifide/gradle/aem/pkg/tasks/Compose.kt) task instance,
* `config` - [Config](src/main/kotlin/com/cognifide/gradle/aem/config/Config.kt) object,
* `rootProject` - project with directory in which *settings.gradle* is located,
* `project` - current project.

This feature is especially useful to generate valid *META-INF/properties.xml* file, below is used by plugin by default:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
    <comment>{{project.description}}</comment>
    <entry key="group">{{compose.vaultGroup}}</entry>
    <entry key="name">{{compose.vaultName}}</entry>
    <entry key="version">{{compose.vaultVersion}}</entry>
    <entry key="description">{{project.description}}</entry>
    <entry key="groupId">{{project.group}}</entry>
    <entry key="artifactId">{{project.name}}</entry>
    <entry key="createdBy">{{user.name}}</entry>
    {% for e in compose.vaultProperties %}
    <entry key="{{e.key}}">{{e.value | raw}}</entry>
    {% endfor %}
</properties>
```

Also file *nodetypes.cnd* is dynamically expanded to generate file containing all node types from all sub packages being merged into assembly package.

Each JAR file in separate *hooks* directory will be combined into single directory when creating assembly package.

#### Task `aemDeploy` 

Upload & install CRX package into AEM instance(s). Primary, recommended form of deployment. Optimized version of `aemUpload aemInstall`.

##### Deploying only to author or publish instances

```bash
gradlew aemDeploy -Paem.instance.authors
gradlew aemDeploy -Paem.instance.publishers
```

##### Deploying only to instances specified explicitly

Instance urls must be delimited by semicolon:

```bash
gradlew aemDeploy -Paem.instance.list=[http://admin:admin@localhost:4502,http://admin:admin@localhost:4503]
```

#### Task `aemUpload`

Upload composed CRX package into AEM instance(s).

#### Task `aemDelete`

Delete uploaded CRX package from AEM instance(s).

#### Task `aemInstall`

Install uploaded CRX package on AEM instance(s).

#### Task `aemUninstall`

Uninstall uploaded CRX package on AEM instance(s).

To prevent data loss, this unsafe task execution must be confirmed by parameter `-Paem.force`.

#### Task `aemPurge` 

Fail-safe combination of `aemUninstall` and `aemDelete`.

To prevent data loss, this unsafe task execution must be confirmed by parameter `-Paem.force`.

#### Task `aemActivate` 

Replicate installed CRX package to other AEM instance(s).

### Bundle plugin

Should be applied to all projects that are composing CRX packages from both *OSGi bundle* being built and optionally *JCR content*, 

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

*aem/app.core/build.gradle.kts*
```kotlin
aem {
    tasks {
        bundle {
            javaPackage = "${project.group}.${project.name}" // "com.company.example.aem.app.core"
        }
    }
}
```

Gradle AEM Plugin is following strategy [convention over configuration](https://pl.wikipedia.org/wiki/Convention_Over_Configuration), which means when following built-in convention about project structure & naming, then only minimal configuration is required. 
Still all features are fully configurable.

#### Embedding JAR file into built OSGi bundle

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
 
For the reference, see [usage in AEM Multi-Project Example](https://github.com/Cognifide/gradle-aem-multi/blob/master/aem/app.common/build.gradle.kts).

#### Configuring OSGi bundle manifest attributes

Plugin by default covers generation of few attributes by convention. The minimal need is to only specify *javaPackage*  in *bundle* section:


```kotlin
aem {
    tasks {
        bundle {
            javaPackage = "com.company.aem.example.common"
        }
    }
}
```

Then the following attributes will be generated by convention:

* `Bundle-Name` will grab value from `project.description`
* `Bundle-SymbolicName` will grab value from `javaPackage`
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
            excludePackages(listOf("org.junit", "org.mockito"))
            importPackages(listOf("!org.junit", "!org.mockito", "*")) // alternatively
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

Provides instance related tasks: `aemAwait`, `aemSetup`, `aemCreate` etc.

Should be applied only at root project / only once within whole build.

Inherits from [Config Plugin](#config-plugin).

#### Task `aemSetup`

Perform initial setup of local AEM instance(s). Automated version of `aemCreate aemUp aemSatisfy aemDeploy`.

![Setup task](docs/setup-task.png)

#### Task `aemResetup`

Combination of `aemDown aemDestroy aemSetup`. Allows to quickly back to initial state of local AEM instance(s).

To prevent data loss, this unsafe task execution must be confirmed by parameter `-Paem.force`.

#### Task `aemCreate`
 
Create AEM instance(s) at local file system. Extracts *crx-quickstart* from downloaded JAR and applies configuration according to [instance definitions](#defining-instances-via-properties-file). 

##### Source files configuration

To use this task, specify required properties in ignored file *gradle.properties* at project root (protocols supported: SMB, SSH, HTTP(s) or local path, HTTP with basic auth as example):

To create instances from backup created by `aemBackup` task, specify:

* `aem.localInstance.zipUrl=http://[user]:[password]@[host]/[path]/example-yyyyMMddmmss-x.x.x-backup.zip`

To create instances from scratch, specify:

* `aem.localInstance.jarUrl=http://[user]:[password]@[host]/[path]/cq-quickstart.jar`
* `aem.localInstance.licenseUrl=http://[user]:[password]@[host]/[path]/license.properties`

Source mode, can be adjusted by specifying parameter `-Paem.localInstance.source`:

* `auto` - Create instances from most recent backup (external or internal) or fallback to creating from the scratch if there is no backup available.
* `none` - Force creating instances from the scratch.
* `backup_external` - Force using backup available at external source (specified in `aem.localInstance.zipUrl`).      
* `backup_internal` - Force using internal backup (created by task `aemBackup`).

When mode is set to `auto` or `backup_internal`, then ZIP selection rule could be adjusted:

```kotlin

aem {
    tasks {
        create {
            options {
                zipSelector = {  // default implementation below
                    val name = aem.props.string("aem.localInstance.zipName") ?: ""
                    when {
                        name.isNotBlank() -> firstOrNull { it.name == name }
                        else -> sortedByDescending { it.name }.firstOrNull()
                    }
                }
            }
        }
    }
}

```

##### Extracted files configuration (optional)

Plugin allows to override or provide extra files to local AEM instance installations.
This behavior is controlled by:

```kotlin
aem {
    config {
        instanceRoot = "${System.getProperty("user.home")}/.aem/${project.rootProject.name}"
    }
    tasks {
        create {
            options {
                overridesPath = "${project.rootProject.file("src/main/resources/local-instance")}"
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
    }
}
```

Properties:

* *aem.config.instanceRoot* determines where AEM instance files will be extracted on local file system.
* *overridesPath* determines project location that holds extra instance files that will override plugin defaults (start / stop scripts) and / or extracted AEM files.
* *expandFiles* specifies which AEM instance files have an ability to use [expandable properties](#expandable-properties) inside.
* *expandProperties* is a place for defining custom properties that can be expanded in AEM instance files.

Predefined expandable properties:

* `instance` - [LocalInstance](src/main/kotlin/com/cognifide/gradle/aem/instance/LocalInstance.kt) object.


#### Task `aemBackup`

Turns off local AEM instance(s) then archives them into ZIP file, then turns on again.

The most recent file created by this task will be reused automatically while running task `aemResetup`.
Also the file created could be also a [source file](#source-files-configuration) for task `aemCreate`.

#### Task `aemDestroy` 

Destroy local AEM instance(s).

To prevent data loss, this unsafe task execution must be confirmed by parameter `-Paem.force`.
    
#### Task `aemUp`

Turn on local AEM instance(s).

#### Task `aemDown`

Turn off local AEM instance(s).

#### Task `aemRestart`

Turn off then on local AEM instance(s).

#### Task `aemReload`

Reload OSGi Framework (Apache Felix) on local and remote AEM instance(s).

#### Task `aemSatisfy` 

Upload & install dependent CRX package(s) before deployment. Available methods:

* `local(path: String)`, use CRX package from local file system.
* `local(file: File)`, same as above, but file can be even located outside the project.
* `url(url: String)`, use CRX package that will be downloaded from specified URL to local temporary directory.
* `downloadHttp(url: String)`, download package using HTTP with no auth.
* `downloadHttpAuth(url: String, username: String, password: String)`, download package using HTTP with Basic Auth support.
* `downloadHttpAuth(url: String)`, as above, but credentials must be specified in variables: `aem.http.username`, `aem.http.password`. Optionally enable SSL errors checking by setting property `aem.http.ignoreSSL` to `false`.
* `downloadSmbAuth(url: String, domain: String, username: String, password: String)`, download package using SMB protocol.
* `downloadSmbAuth(url: String)`, as above, but credentials must be specified in variables: `aem.smb.domain`, `aem.smb.username`, `aem.smb.password`.
* `downloadSftpAuth(url: String, username: String, password: String)`, download package using SFTP protocol.
* `downloadSftpAuth(url: String)`, as above, but credentials must be specified in variables: `aem.sftp.username`, `aem.sftp.password`. Optionally enable strict host checking by setting property `aem.sftp.hostChecking` to `true`.
* `dependency(notation: String)`, use OSGi bundle that will be resolved from defined repositories (for instance from Maven) then wrapped to CRX package: `dependency('com.neva.felix:search-webconsole-plugin:1.2.0')`.
* `group(name: String, options: Resolver<PackageGroup>.() -> Unit)`, useful for declaring group of packages (or just optionally naming single package) to be installed only on demand. For instance: `group 'tools', { url('http://example.com/package.zip'); url('smb://internal-nt/package2.zip')  }`. Then to install only packages in group `tools`, use command: `gradlew aemSatisfy -Paem.satisfy.group=tools`.

Example configuration:

```kotlin
aem {
    tasks {
        satisfy {
            group("default") {
                local("pkg/vanityurls-components-1.0.2.zip")
                url("smb://company-share/aem/packages/my-lib.zip")
                url("sftp://company-share/aem/packages/other-lib.zip")
                url("file:///C:/Libraries/aem/package/extra-lib.zip")
            }
            
            group("tools") {
                dependency("com.neva.felix:search-webconsole-plugin:1.2.0")
                url("https://github.com/Cognifide/APM/releases/download/cqsm-3.0.0/apm-3.0.0.zip")
                url("https://github.com/Adobe-Consulting-Services/acs-aem-tools/releases/download/acs-aem-tools-1.0.0/acs-aem-tools-content-1.0.0-min.zip")
            }
        }
    }
}
```

By default, all packages will be deployed when running task `aemSatisfy`.
Although, by grouping packages, there are available new options:

* group name could be used to filter out packages that will be deployed (`-Paem.satisfy.group=tools`, wildcards supported, comma delimited).
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
                    url("https://github.com/OlsonDigital/aem-groovy-console/releases/download/11.0.0/aem-groovy-console-11.0.0.zip")
                    config {
                        instanceName = "*-author" // additional filter intersecting 'deployInstanceName'
                        initializer { sync ->
                            logger.info("Installing Groovy Console on ${sync.instance}")
                        }
                        finalizer { sync ->
                            logger.info("Installed Groovy Console on ${sync.instance}")
                        }
                        completer {
                            logger.info("Reloading instance(s) after installing Groovy Console")
                            aem.actions.reload {
                                delay = 3
                            }
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
gradlew aemSatisfy -Paem.satisfy.urls=[url1,url2]
```

For instance:

```bash
gradlew aemSatisfy -Paem.satisfy.urls=[https://github.com/OlsonDigital/aem-groovy-console/releases/download/11.0.0/aem-groovy-console-11.0.0.zip,https://github.com/neva-dev/felix-search-webconsole-plugin/releases/download/search-webconsole-plugin-1.2.0/search-webconsole-plugin-1.2.0.jar]
```

#### Task `aemAwait`

Wait until all local or remote AEM instance(s) be stable.

Action parameter | CMD Property | Default Value | Purpose
--- | --- | --- | ---
`stableRetry` | *aem.await.stableRetry* | `300` | Hook for customizing how often and how many stability checks will be performed. Corresponding CMD param controls maximum count of retries if default hook is active.
`stableAssurance` | *aem.await.stableAssurance* | `3` | Number of intervals / additional instance stability checks to assure all stable instances.
`stableCheck` | n/a | `{ it.checkBundleStable() }` | Hook for customizing instance stability check. Check will be repeated if assurance is configured. 
`healthCheck` | n/a | { `it.checkComponentState() }` | Hook for customizing instance health check.
`healthRetry` | *aem.await.healthRetry* | `5` | Hook for customizing how often and how many health checks will be performed.
`fast` | *aem.await.fast* | `false` | Skip stable check assurances and health checking. Alternative, quicker type of awaiting stable instances.
`fastDelay` | *aem.await.fastDelay* | `1000` | Time in milliseconds to postpone instance stability checks to avoid race condition related with actual operation being performed on AEM like starting JCR package installation or even creating launchpad.  Considered only when fast mode is enabled.
`warmupDelay` | *aem.await.warmupDelay* | `0` | Time to wait e.g after deployment before checking instance stability. Considered only when fast mode is disabled.
`resume` | *aem.await.resume* | `false` | Do not fail build but log warning when there is still some unstable or unhealthy instance.

Instance state, stable check, health check lambdas are using: [InstanceState](src/main/kotlin/com/cognifide/gradle/aem/instance/InstanceState.kt). Use its methods to achieve expected customized behavior.

#### Task `aemCollect`

Composes ZIP package from all CRX packages being satisfied and built.

Inherits from task [ZIP](https://docs.gradle.org/3.5/dsl/org.gradle.api.tasks.bundling.Zip.html).

Screenshot below presents generated ZIP package which is a result of running `gradlew :aemCollect` for [multi-module project](https://github.com/Cognifide/gradle-aem-multi).

![Collect task - ZIP Overview](docs/collect-zip-overview.png)

## How to's

### Set AEM configuration properly for all / concrete project(s)

Common configuration like root of content for JCR package, should be defined in `allprojects` section like below / e.g in root *build.gradle.kts* file:

```kotlin
allprojects {
  plugins.withId("com.cognifide.aem.base") {
    configure<AemExtension> {
        config {
            packageRoot = file("src/main/aem") // overrides default dir named 'content'
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
        "compileOnly"("com.adobe.aem:uber-jar:6.4.0:obfuscated-apis") // and more
    }
  }
}
```

For instance, subproject `:aem:core` specific configuration like OSGi bundle or CRX package options should be defined in `aem/core/build.gradle.kts`:

```kotlin
aem {
    tasks {
        compose {
            fromProjects(':content:*')
            baseName = "example-core"
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
        bundle {
            javaPackage = "com.company.example.aem.core"
        }
    }
}
```

Warning! Very often plugin users mistake is to configure `aemSatisfy` task in `allprojects` closure. 
As an effect there will be same dependent CRX package defined multiple times.

### Understand why there are one or two plugins to be applied in build script

Gradle AEM Plugin assumes separation of 4 plugins to properly fit into Gradle tasks structure correctly.

Most often, Gradle commands are being launched from project root and tasks are being run by their name e.g `aemSatisfy` (which is not fully qualified, better if it will be `:aemSatisfy` of root project).
Let's imagine if task `aemSatisfy` will come from package plugin, then Gradle will execute more than one `aemSatisfy` (for all projects that have plugin applied), so that this is unintended behavior.
Currently used plugin architecture solves that problem.

### Work effectively on start and daily basis

Initially, to create fully configured local AEM instances simply run command `gradlew aemSetup`.

Later during development process, building and deploying to AEM should be done using most simple command: `gradlew`.
Above configuration uses [default tasks](https://docs.gradle.org/current/userguide/tutorial_using_tasks.html#sec:default_tasks), so that alternatively it is possible to build same using explicitly specified command `gradlew aemSatisfy aemDeploy aemAwait`.

* Firstly dependent packages (like AEM hotfixes, Vanity URL Components etc) will be installed lazily (only when they are not installed yet).
* In next step application is being built and deployed to all configured AEM instances.
* Finally build awaits till all AEM instances be stable.

### Filter instances for which packages will be deployed or satisfied

When there are defined named AEM instances: `local-author`, `local-publish`, `integration-author` and `integration-publish`,
then it is available to deploy (or satisfy) packages with taking into account: 

 * type of environment (local, integration, staging, etc)
 * type of AEM instance (author / publish)

Example cases:

```bash
gradlew aemDeploy -Paem.instance.name=integration-*
gradlew aemDeploy -Paem.instance.name=*-author
gradlew aemDeploy -Paem.instance.name=local-author,integration-author
```

Default value of that instance name filter is `${aem.environment}-*`, so that typically `local-*`.
Environment value comes from system environment variable `AEM_ENV` or property `aem.env`.

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

### Caching task `aemCompose`

Expandable properties with dynamically calculated value (unique per build) like `created` and `buildCount` are not used by default generated properties file intentionally, 
because such usages will effectively forbid caching `aemCompose` task and it will be never `UP-TO-DATE`.

### Vault tasks parallelism

Vault tool current working directory cannot be easily configured, because of its API. AEM plugin is temporarily changing current working directory for Vault, then returning it back to original value.
In case of that workaround, Vault tasks should not be run in parallel (by separated daemon processed / JVM synchronization bypassed), because of potential unpredictable behavior.

### Files from SSH for `aemCreate` and `aemSatisfy`

Local instance JAR file can be provided using SSH, but SSHJ client used in implementation has an [integration issue](https://github.com/hierynomus/sshj/issues/347) related with JDK and Crypto Policy.
As a workaround, just run build without daemon (`--no-daemon`).

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
