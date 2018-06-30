![Cognifide logo](docs/cognifide-logo.png)

[![Gradle Status](https://gradleupdate.appspot.com/Cognifide/gradle-aem-plugin/status.svg)](https://gradleupdate.appspot.com/Cognifide/gradle-aem-plugin/status)
[![Apache License, Version 2.0, January 2004](docs/apache-license-badge.svg)](http://www.apache.org/licenses/)
![Travis Build](https://travis-ci.org/Cognifide/gradle-aem-plugin.svg?branch=develop)

# Gradle AEM Plugin

<p align="center">
  <img src="docs/logo.png" alt="Gradle AEM Plugin Logo"/>
</p>

## Description

Currently there is no popular way to build applications for AEM using Gradle build system. This project contains brand new Gradle plugin to assemble CRX package and deploy it on instance(s).

Incremental build which takes seconds, not minutes. Developer who does not loose focus between build time gaps. Extend freely your build system directly in project. 

AEM developer - it's time to meet Gradle! You liked or used plugin? Don't forget to **star this project** on GitHub :)

<br>
<p align="center">
  <img src="docs/example-build.gif" alt="Example Project Build"/>
</p>
<br>

**Features:**

* Fully automated, tied to project, local AEM instance(s) setup allowing to start development within few minutes.
* Composing CRX package from multiple JCR content roots, bundles.
* Automated all-in-one CRX packages generation (assemblies).
* Easy multi-deployment with instance groups.
* Automated dependent CRX packages and OSGi bundles installation from local and remote sources (SMB, SSH, HTTP(s)).
* Smart Vault files generation (combining defaults with overiddables).
* Embedded Vault tool for checking out and cleaning JCR content from running AEM instance.
* OSGi Manifest customization by embedded [BND plugin](https://github.com/bndtools/bnd/tree/master/biz.aQute.bnd.gradle).
* OSGi Declarative Services annotations support (instead of SCR, [see docs](http://blogs.adobe.com/experiencedelivers/experience-management/osgi/using-osgi-annotations-aem6-2/)).

## Table of contents

- [Gradle AEM Plugin](#gradle-aem-plugin)
    - [Description](#description)
    - [Table of contents](#table-of-contents)
    - [Installation](#installation)
    - [Configuration](#configuration)
        - [Plugin setup](#plugin-setup)
            - [Minimal:](#minimal)
            - [Additional](#additional)
        - [Base plugin tasks](#base-plugin-tasks)
            - [Task `aemSync`](#task-aemsync)
            - [Task `aemCheckout`](#task-aemcheckout)
            - [Task `aemClean`](#task-aemclean)
            - [Task `aemVlt`](#task-aemvlt)
            - [Task `aemRcp`](#task-aemrcp)
            - [Task `aemDebug`](#task-aemdebug)
        - [Package plugin tasks](#package-plugin-tasks)
            - [Task `aemCompose`](#task-aemcompose)
            - [Task `aemDeploy`](#task-aemdeploy)
            - [Task `aemUpload`](#task-aemupload)
            - [Task `aemDelete`](#task-aemdelete)
            - [Task `aemInstall`](#task-aeminstall)
            - [Task `aemUninstall`](#task-aemuninstall)
            - [Task `aemPurge`](#task-aempurge)
            - [Task `aemActivate`](#task-aemactivate)
        - [Instance plugin tasks](#instance-plugin-tasks)
            - [Task `aemSetup`](#task-aemsetup)
            - [Task `aemCreate`](#task-aemcreate)
            - [Task `aemDestroy`](#task-aemdestroy)
            - [Task `aemUp`](#task-aemup)
            - [Task `aemDown`](#task-aemdown)
            - [Task `aemReload`](#task-aemreload)
            - [Task `aemSatisfy`](#task-aemsatisfy)
            - [Task `aemAwait`](#task-aemawait)
            - [Task `aemCollect`](#task-aemcollect)
        - [Expandable properties](#expandable-properties)
    - [How to's](#how-tos)
        - [Set AEM configuration properly for all / concrete project(s)](#set-aem-configuration-properly-for-all---concrete-projects)
        - [Work with local and/or remote AEM instances](#work-with-local-and-or-remote-aem-instances)
        - [Understand why there are one or two plugins to be applied in build script](#understand-why-there-are-one-or-two-plugins-to-be-applied-in-build-script)
        - [Work effectively on start and daily basis](#work-effectively-on-start-and-daily-basis)
        - [Deploy CRX package(s) only to filtered group of instances:](#deploy-crx-packages-only-to-filtered-group-of-instances)
        - [Deploy CRX package(s) only to instances specified explicitly](#deploy-crx-packages-only-to-instances-specified-explicitly)
        - [Deploy only filtered dependent CRX package(s)](#deploy-only-filtered-dependent-crx-packages)
        - [Customize local AEM instances configuration](#customize-local-aem-instances-configuration)
        - [Check out and clean JCR content using filter at custom path](#check-out-and-clean-jcr-content-using-filter-at-custom-path)
        - [Check out and clean JCR content using filter roots specified explicitly](#check-out-and-clean-jcr-content-using-filter-roots-specified-explicitly)
        - [Assemble all-in-one CRX package(s)](#assemble-all-in-one-crx-packages)
        - [Include additional OSGi bundle into CRX package](#include-additional-osgi-bundle-into-crx-package)
        - [Embed JAR file into built OSGi bundle](#embed-jar-file-into-built-osgi-bundle)
        - [Skip installed package resolution by download name.](#skip-installed-package-resolution-by-download-name)
    - [Contributing](#contributing)
        - [Basics](#basics)
         - [Testing local version of plugin](#testing-local-version-of-plugin)
         - [Vault tasks parallelism](#vault-tasks-parallelism)
            
    - [Known issues](#known-issues)
        - [No OSGi services / components are registered](#no-osgi-services---components-are-registered)
        - [Caching task `aemCompose`](#caching-task-aemcompose)
        - [Vault tasks parallelism](#vault-tasks-parallelism)
        - [Files from SSH for `aemCreate` and `aemSatisfy`](#files-from-ssh-for-aemcreate-and-aemsatisfy)
    - [License](#license)

## Installation

* Most effective way to experience Gradle AEM Plugin is to use Quickstart located in:
  * [AEM Single-Project Example](https://github.com/Cognifide/gradle-aem-single#quickstart) - recommended for **application** development,
  * [AEM Multi-Project Example](https://github.com/Cognifide/gradle-aem-multi#quickstart) - recommended for **project** development,
* The only needed software to start using plugin is to have installed on machine Java 8.
* As a build command, it is recommended to use Gradle Wrapper (`gradlew`) instead of locally installed Gradle (`gradle`) to easily have same version of build tool installed on all environments. Only at first build time, wrapper will be automatically downloaded and installed, then reused.

## Configuration

### Plugin setup

Released versions of plugin are available on [Bintray](https://bintray.com/cognifide/maven-public/gradle-aem-plugin), 
so that this repository need to be included in *buildscript* section.

#### Minimal:

Configuration below assumes building and deploying on AEM instance(s) via command: `gradlew aemDeploy`.

File *settings.gradle*:
```groovy
pluginManagement {
	repositories {
		jcenter()
		maven { url  "http://dl.bintray.com/cognifide/maven-public" }
	}
	resolutionStrategy {
		eachPlugin {
			if (requested.id.namespace == 'com.cognifide.aem') {
				useModule('com.cognifide.gradle:aem-plugin:4.0.5')
			}
		}
	}
}
```

File *build.gradle*:
```groovy
plugins {
	id 'com.cognifide.aem.bundle' // or 'package' for JCR content only
	
}
```

#### Additional

AEM configuration section contains all default values for demonstrative purpose.

**More detailed and always up-to-date information about all configuration options is available [here](src/main/kotlin/com/cognifide/gradle/aem/api/AemConfig.kt).**

Configuration below assumes building and deploying on AEM instance(s) via command: `gradlew` (default tasks will be used).

```groovy
plugins {
	id 'com.cognifide.aem.bundle'
	id 'com.cognifide.aem.instance'
	id 'org.jetbrains.kotlin.jvm' // or any other like 'java' to compile OSGi bundle
}

defaultTasks = [':aemSatisfy', ':aemDeploy', ':aemAwait']

aem {
    config {
        environment = "local" // -Paem.env or environment variable: AEM_ENV
    
        instanceName = "${environment}-*"
        instanceAuthorName = "${environment}-author"
        instanceConnectionTimeout = 5000
        instanceConnectionUntrustedSsl = true
    
        contentPath = project.file("src/main/content")
        if (project == project.rootProject) {
            bundlePath = "/apps/${project.name}/install"
        } else {
            bundlePath = "/apps/${project.rootProject.name}/${project.name}/install"
        }
        bundlePackage = ""
        bundlePackageOptions = "-split-package:=merge-first"
        bundleManifestAttributes = true
        bundleBndPath = "${project.file('bnd.bnd')}"
    
        if (projectUniqueName) {
            packageName = project.name
        } else {
            packageName = "${projectNamePrefix}-${project.name}"
        }
        packageLocalPath = ""
        packageRemotePath = ""
        packageFilesExcluded = [
          "**/.gradle",
          "**/.git",
          "**/.git/**",
          "**/.gitattributes",
          "**/.gitignore",
          "**/.gitmodules",
          "**/.vlt",
          "**/node_modules/**",
          "jcr_root/.vlt-sync-config.properties"
        ]
        packageFilesExpanded = [
          "**/META-INF/vault/*.xml"
        ]
        packageFileProperties = []
        packageBuildDate = Date()
        packageAcHandling = "merge_preserve"
        packageSnapshots = []
        
        vaultCopyMissingFiles = true
        vaultFilesPath = project.rootProject.file("src/main/resources/META-INF/vault")
        vaultLineSeparator = "LF"
    
        deployDistributed = false
        
        uploadForce = true
        uploadRetryTimes = 6
        uploadRetryDelay = 30000
        
        installRecursive = true
        installRetryTimes = 3
        installRetryDelay = 30000
        
        createPath = "${System.getProperty("user.home")}/.aem/${project.rootProject.name}"
        createFilesPath = project.rootProject.file("src/main/resources/local-instance")
        createFilesExpanded = [
          "**/*.properties", 
          "**/*.sh", 
          "**/*.bat", 
          "**/*.xml",
          "**/start",
          "**/stop"
        ]
        
        upInitializer = { handle -> }
        
        awaitStableDelay = 3000
        awaitStableInterval = 1000
        awaitStableTimes = 300
        awaitStableAssurances = 3
        awaitStableState = { it.checkBundleState(500) }
        awaitStableCheck = { it.checkBundleStable(500) }
        awaitHealthCheck = { it.checkComponentState(["com.day.crx.packaging.*", "org.apache.sling.installer.*"], 10000) }
        awaitHealthRetryTimes = 3L
        awaitHealthRetryDelay = 30000
        awaitFast = false
        awaitFastDelay = 1000
        awaitResume = false
        
        reloadDelay = 10000
        
        satisfyRefreshing = false
        satisfyBundlePath = "/apps/gradle-aem-plugin/satisfy/install"
        satisfyBundleProperties = { bundle -> [:] }
        satisfyGroupName = "*"
        
        checkoutFilterPath = ""
        
        cleanFilesDeleted = [
            "**/.vlt",
            "**/.vlt*.tmp",
            "**/jcr_root/.content.xml",
            "**/jcr_root/apps/.content.xml",
            "**/jcr_root/conf/.content.xml",
            "**/jcr_root/content/.content.xml",
            "**/jcr_root/content/dam/.content.xml",
            "**/jcr_root/etc/.content.xml",
            "**/jcr_root/etc/designs/.content.xml",
            "**/jcr_root/home/.content.xml",
            "**/jcr_root/home/groups/.content.xml",
            "**/jcr_root/home/users/.content.xml",
            "**/jcr_root/libs/.content.xml",
            "**/jcr_root/system/.content.xml",
            "**/jcr_root/tmp/.content.xml",
            "**/jcr_root/var/.content.xml"
      ]
      cleanSkipProperties = [
        "jcr:uuid!**/home/users/*,**/home/groups/*",
        "jcr:lastModified",
        "jcr:created",
        "cq:lastModified*",
        "cq:lastReplicat*",
        "*_x0040_Delete",
        "*_x0040_TypeHint"
      ]
      
      notificationEnabled = false
    }
}
```

### Base plugin tasks

#### Task `aemSync`

Check out then clean JCR content.

#### Task `aemCheckout`

Check out JCR content from running AEM author instance to local content path.

#### Task `aemClean`

Clean checked out JCR content.

#### Task `aemVlt`

Execute any JCR File Vault command. 

For instance, to copy nodes from one remote AEM instance to another, there might be used command below:

```bash
gradlew :content:aemVlt -Paem.vlt.command='rcp -b 100 -r -u -n http://admin:admin@localhost:4502/crx/-/jcr:root/content/dam/example http://admin:admin@localhost:4503/crx/-/jcr:root/content/dam/example' 
```

For more details about available parameters, please visit [VLT Tool documentation](https://docs.adobe.com/docs/en/aem/6-2/develop/dev-tools/ht-vlttool.html).

While using task `aemVlt` be aware that Gradle requires to have working directory with file *build.gradle* in it, but Vault tool can work at any directory under *jcr_root*. To change working directory for Vault, use property `aem.vlt.path` which is relative path to be appended to *jcr_root* for project task being currently executed.

#### Task `aemRcp`

Copy JCR content from one instance to another. Sample usages below.

Using predefined instances with multiple different source and target nodes:
```
gradlew :aemRcp -Paem.rcp.source.instance=int-author -Paem.rcp.target.instance=local-author -Paem.rcp.paths=[/content/example-demo=/content/example,/content/dam/example-demo=/content/dam/example]
```

Using predefined instances with multiple same source and target nodes:
```
gradlew :aemRcp -Paem.rcp.source.instance=stg-author -Paem.rcp.target.instance=int-author -Paem.rcp.paths=[/content/example,/content/example2]
```

Using dynamically defined instances:
```
gradlew :aemRcp -Paem.rcp.source.instance=http://user:pass@192.168.66.66:4502 -Paem.rcp.target.instance=http://user:pass@192.168.33.33:4502 -Paem.rcp.paths=[/content/example-demo=/content/example]
```

Keep in mind, that copying JCR content between instances, could be a trigger for running AEM workflows like *DAM Update Asset* which could cause heavy load on instance.
Consider disabling AEM workflow launchers before running this task and re-enabling after.

RCP task is internally using [Vault Remote Copy](http://jackrabbit.apache.org/filevault/rcp.html) which requires to having bundle *Apache Sling Simple WebDAV Access to repositories (org.apache.sling.jcr.webdav)* " in active state on instance.

#### Task `aemDebug` 

Dumps effective AEM build configuration of concrete project to JSON file.

When command below is being run (for root project `:`):

```bash
gradlew :aemDebug
```

Then file at path *build/aem/aemDebug/debug.json* with content below is being generated:

```javascript
{
  "buildInfo" : {
    "plugin" : {
      "pluginVersion" : "x.y.z",
      "gradleVersion" : "x.y.z"
    }
  },
  "projectInfo" : {
    "displayName" : "root project 'example'",
    "path" : ":",
    "name" : "example",
    "dir" : "C:\\Users\\krystian.panek\\Projects\\gradle-aem-multi"
  },
  "packageProperties" : {
    "name" : "example",
    "config" : {
      "instances" : {
        "local-author" : {
          "httpUrl" : "http://localhost:4502",
          "user" : "admin",
          "password" : "admin",
          "typeName" : "author",
          "debugPort" : 14502,
          "name" : "local-author",
          "type" : "AUTHOR",
          "httpPort" : 4502,
          "environment" : "local"
        }
        // ...
      },
      "uploadForce" : true,
      "installRecursive" : true
      // ...
    },
    "requiresRoot" : "false",
    "buildCount" : "20173491654283",
    "created" : "2017-12-15T07:16:54Z"
  },
  "packageDeployed" : {
    "local-author" : {
      "group" : "com.company.aem",
      "name" : "example",
      "version" : "1.0.0-SNAPSHOT",
      "path" : "/etc/packages/com.company.aem/example-1.0.0-SNAPSHOT.zip",
      "downloadName" : "example-1.0.0-SNAPSHOT.zip",
      "lastUnpacked" : 1513321701062,
      "installed" : true
    }
    // ...
  }
}
```
### Package plugin tasks

#### Task `aemCompose`

Compose CRX package from JCR content and bundles. Available methods:

* `includeProject(projectPath: String)`, includes both bundles and JCR content from another project, example: `includeProject ':core'`.
* `includeContent(projectPath: String)`, includes only JCR content, example: `includeContent ':design'`.
* `includeBundles(projectPath: String)`, includes only bundles, example: `includeBundles ':common'`.
* `includeBundlesAtPath(projectPath: String, installPath: String)`, includes only bundles at custom install path, example: `includeBundles(':common', '/apps/my-app/install')`.
* `includeBundles(projectPath: String, runMode: String)`, as above, useful when bundles need to be installed only on specific type of instance.
* `mergeBundles(projectPath: String)`, includes only bundles at same install path.
* `mergeBundles(projectPath: String, runMode: String)`, as above, useful when bundles need to be installed only on specific type of instance.
* `includeProjects(pathPrefix: String)`, includes both bundles and JCR content from all AEM projects (excluding itself) in which project path is matching specified filter. Vault filter roots will be automatically merged and available in property `${filterRoots}` in *filter.xml* file. Useful for building assemblies (all-in-one packages).
* `includeSubprojects()`, alias for method above: `includeProjects("${project.path}:*")`.
* all inherited from [ZIP task](https://docs.gradle.org/3.5/dsl/org.gradle.api.tasks.bundling.Zip.html).

#### Task `aemDeploy` 

Upload & install CRX package into AEM instance(s). Primary, recommended form of deployment. Optimized version of `aemUpload aemInstall`.

#### Task `aemUpload`

Upload composed CRX package into AEM instance(s).

#### Task `aemDelete`

Delete uploaded CRX package from AEM instance(s).

#### Task `aemInstall`

Install uploaded CRX package on AEM instance(s).

#### Task `aemUninstall`

Uninstall uploaded CRX package on AEM instance(s).

#### Task `aemPurge` 

Fail-safe combination of `aemUninstall` and `aemDelete`.

#### Task `aemActivate` 

Replicate installed CRX package to other AEM instance(s).

### Instance plugin tasks

#### Task `aemSetup`

Perform initial setup of local AEM instance(s). Automated version of `aemCreate aemUp aemSatisfy aemDeploy`.

![Setup task](docs/setup-task.png)

#### Task `aemCreate`
 
Create local AEM instance(s). To use it specify required properties in ignored file *gradle.properties* at project root (protocols supported: SMB, SSH, HTTP(s) or local path, SMB as example):

* `aem.instance.local.jarUrl=smb://[host]/[path]/cq-quickstart.jar`
* `aem.instance.local.licenseUrl=smb://[host]/[path]/license.properties`
* `aem.smb.domain=MYDOMAIN`
* `aem.smb.username=MYUSER`
* `aem.smb.password=MYPASSWORD`
  
#### Task `aemDestroy` 

Destroy local AEM instance(s).
    
#### Task `aemUp`

Turn on local AEM instance(s).

#### Task `aemDown`

Turn off local AEM instance(s).

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
* `group(name: String, configurer: Closure)`, useful for declaring group of packages (or just optionally naming single package) to be installed only on demand. For instance: `group 'tools', { url('http://example.com/package.zip'); url('smb://internal-nt/package2.zip')  }`. Then to install only packages in group `tools`, use command: `gradlew aemSatisfy -Paem.satisfy.group=tools`.

Example configuration:

```groovy
aemSatisfy {
    local "pkg/vanityurls-components-1.0.2.zip"
    url "https://github.com/Cognifide/APM/releases/download/cqsm-3.0.0/apm-3.0.0.zip"
    url "smb://company-share/aem/packages/my-lib.zip"
    url "sftp://company-share/aem/packages/other-lib.zip"
    url "file:///C:/Libraries/aem/package/extra-lib.zip"
    dependency 'com.neva.felix:search-webconsole-plugin:1.2.0'
}
```

It is also possible to specify packages to be deployed only once via command line parameter. Also for local files at any file system paths.

```bash
gradlew aemSatisfy -Paem.satisfy.urls=[url1,url2]
```

For instance:

```bash
gradlew aemSatisfy -Paem.satisfy.urls=[https://github.com/OlsonDigital/aem-groovy-console/releases/download/11.0.0/aem-groovy-console-11.0.0.zip,https://github.com/neva-dev/felix-search-webconsole-plugin/releases/download/search-webconsole-plugin-1.2.0/search-webconsole-plugin-1.2.0.jar]
```

#### Task `aemAwait`

Wait until all local or remote AEM instance(s) be stable.

AEM Config Param | CMD Property | Default Value | Purpose
--- | --- | --- | ---
`awaitStableInterval` | *aem.await.stable.interval* | `1000` | Time in milliseconds used as interval between next instance stability checks being performed. Optimization could be necessary only when instance is heavily loaded.
`awaitStableTimes` | *aem.await.stable.times* | `300` | Maximum intervals after which instance stability checks will be skipped if there is still some unstable instance left.
`awaitStableAssurances` | *aem.await.stable.assurances* | `3` | Number of intervals / additional instance stability checks to assure all stable instances.
`awaitStableCheck` | n/a | `{ it.checkBundleStable(500) }` | Hook for customizing instance stability check. Check will be repeated if assurance is configured. 
`awaitHealthCheck` | n/a | { `it.checkComponentState(10000) }` | Hook for customizing instance health check.
`awaitHealthRetryTimes` | *aem.await.health.retry.times* | `3` | Repeat health check when failed (brute-forcing).
`awaitHealthRetryDelay` | *aem.await.health.retry.delay* | `30000` | Time to wait after repeating failed health check.
`awaitFast` | *aem.await.fast* | `false` | Skip stable check assurances and health checking. Alternative, quicker type of awaiting stable instances.
`awaitFastDelay` | *aem.await.fast.delay* | `1000` | Time in milliseconds to postpone instance stability checks to avoid race condition related with actual operation being performed on AEM like starting JCR package installation or even creating launchpad.
`awaitResume` | *aem.await.resume* | `false` | Do not fail build but log warning when there is still some unstable or unhealthy instance.

Instance state, stable check, health check lambdas are using: [InstanceState](src/main/kotlin/com/cognifide/gradle/aem/instance/InstanceState.kt). Use its methods to achieve expected customized behavior.

#### Task `aemCollect`

Composes ZIP package from all CRX packages being satisfied and built. Available methods:

* all inherited from [ZIP task](https://docs.gradle.org/3.5/dsl/org.gradle.api.tasks.bundling.Zip.html).

Screenshot below presents generated ZIP package which is a result of running `gradlew :aemCollect` for [multi-module project](https://github.com/Cognifide/gradle-aem-multi).

![Collect task - ZIP Overview](docs/collect-zip-overview.png)

### Expandable properties

By default, plugin is configured that in all XML files located under path *META-INF/vault* properties can be injected using syntax: `{{property}}`.
The properties syntax comes from [Pebble Template Engine](http://www.mitchellbosecke.com/pebble) which means that all its features (if statements, for loops, filters etc) can be used inside files being expanded.

Related configuration:

```groovy
aem {
    config {
        fileProperties = [
            "organization": "My Company"
        ]
        filesExpanded = [
            "**/META-INF/vault/*.xml"
        ]
    }
}
```

This feature is specially useful to generate valid *META-INF/properties.xml* file, below is used by plugin by default:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
    <comment>{{project.description}}</comment>
    <entry key="group">{{project.group}}</entry>
    <entry key="name">{{config.packageName}}</entry>
    <entry key="version">{{project.version}}</entry>
    <entry key="groupId">{{project.group}}</entry>
    <entry key="artifactId">{{project.name}}</entry>
    <entry key="description">{{project.description}}</entry>
    <entry key="createdBy">{{user.name}}</entry>
    <entry key="acHandling">{{config.packageAcHandling}}</entry>
    <entry key="requiresRoot">{{requiresRoot}}</entry>
</properties>
```

Predefined properties:
* `config` - [AEM configuration](src/main/kotlin/com/cognifide/gradle/aem/api/AemConfig.kt).
* `rootProject` - project with directory in which *settings.gradle* is located.
* `project` - current project.
* `buildCount` - number to be used as CRX package build count (`buildDate` in format `yDDmmssSSS`).
* `created` - current date in *ISO8601* format.

Task specific:
* `aemCompose` - properties which are being dynamically calculated basing on content actually included into package.
   * `filterRoots` - after using method `includeContent` of `aemCompose` task, all Vault filter roots are being gathered. This property contains all these XML tags concatenated especially useful for building assemblies. If no projects will be included, then this variable will contain a single filter root with bundle path to be able to deploy auto-generated package with JAR file only.
   * `filters` - same as above, but instead it is a collection of XML elements that could be traversed and rendered in a customized way.
* `aemVlt` - properties are being injected to command specified in `aem.vlt.command` property. Following properties are being used internally also by `aemCheckout`.
   * `instances` - map of defined instances with names as keys. 

## How to's

### Set AEM configuration properly for all / concrete project(s)

Global configuration like AEM instances should be defined in root *build.gradle* file:

```groovy
allprojects { subproject ->
  plugins.withId 'com.cognifide.aem.base', {
    aem {
        config {
          remoteInstance "http://localhost:6502"
          contentPath = subproject.file("src/main/aem")
        }
    }
  }
}
```

For instance, project `:app:core` specific configuration like OSGi bundle or CRX package options should be defined in `app/core/build.gradle`:

```groovy
aem {
    config {
        bundlePackage = 'com.company.aem.example.core'
    }
}

aemCompose {
    includeProjects ':content:*'
    baseName = 'example-core'
    duplicatesStrategy = "EXCLUDE"
}
```

Warning! Very often plugin users mistake is to configure `aemSatisfy` task in `allprojects` closure. 
As an effect there will be same dependent CRX package defined multiple times.

### Work with local and/or remote AEM instances

In AEM configuration section, there is possibility to use `localInstance` or `remoteInstance` methods to define AEM instances to be used to:
 
* install CRX packages being built via command `aemDeploy` or combination of more detailed `aemUpload`, `aemInstall` and optionally `aemActivate`,
* communicate with while using Vault tool in commands `aemSync`, `aemCheckout`, `aemVlt`,
* install dependent packages while using `aemSatisfy` command.

```groovy
aem {
    config {
        localInstance "http://localhost:4502" // local-author
        localInstance "http://localhost:4502", { // local-author
            user = "admin"
            password = "admin"
            typeName = "author"
            debugPort = 14502 
        }
      
        localInstance "http://localhost:4503" // local-publish
        localInstance "http://localhost:4503", { // local-publish
            user = "admin"
            password = "admin"
            typeName = "publish"
            debugPort = 14503
        } 
      
        remoteInstance "http://192.168.10.1:4502", { // integration-author1
            user = "user1" 
            password = "password2"
            environment = "integration"
            typeName = "author1"
        } 
        remoteInstance "http://192.168.10.1:8080", { // integration-author2
            user = "user1" 
            password = "password2"
            environment = "integration"
            typeName = "author2"
        } 
        remoteInstance "http://192.168.10.2:4503", { // integration-publish1
            user = "user2"
            password = "password2"
            environment = "integration"
            typeName = "publish1"
        } 
        remoteInstance "http://192.168.10.2:8080", { // integration-publish2
            user = "user2"
            password = "password2"
            environment = "integration"
            typeName = "publish2"
        } 
    }
}
```

The above configuration can be also specified through *gradle.properties* file using dedicated syntax.

`aem.instance.$TYPE.$ENVIRONMENT-$TYPE_NAME.$PROP_NAME=$PROP_VALUE`

Part | Possible values | Description |
--- | --- | --- |
`$TYPE` | `local` or `remote` (only) |  Type of instance. Local means that for each one there will be set up AEM Quickstart at local file system. | 
`$ENVIRONMENT` | `local`, `int`, `stg` etc | Environment name. |
`$TYPE_NAME` | `author`, `publish`, `publish2`, etc | Combination of AEM instance type and semantic suffix useful when more than one of instance of same type is being configured. |
`$PROP_NAME=$PROP_VALUE` | Local instances: `httpUrl=http://admin:admin@localhost:4502`, `password=foo`, `runModes=nosamplecontent`, `jvmOpts=-server -Xmx2048m -XX:MaxPermSize=512M -Djava.awt.headless=true`, `startOpts=...`, `debugPort=24502`. Remote instances: `httpUrl`, `user`, `password`. | Run modes, JVM opts and start opts should be comma delimited. |

**Rules:**

* Instance name is a combination of *${environment}-${typeName}* e.g *local-author*, *integration-publish* etc.
* Instance type name must start with prefix *author* or *publish*. Sample valid names: *author*, *author1*, *author2*, *author-master* and *publish*, *publish1* *publish2* etc.
* Only instances being defined as *local* are being considered in command `aemSetup`, `aemCreate`, `aemUp` etc (that comes from `com.cognifide.aem.instance` plugin).
* All instances being defined as *local* or *remote* are being considered in commands CRX package deployment related like `aemSatisfy`, `aemDeploy`, `aemUpload`, `aemInstall` etc.

### Understand why there are one or two plugins to be applied in build script

Gradle AEM plugin architecture is splitted into 4 plugins to properly fit into Gradle tasks structure correctly.

* base (`com.cognifide.aem.base`), applied transparently by other plugins, provides AEM config section to build script and general tasks: `aemDebug`, `aemVlt` etc.
* instance (`com.cognifide.aem.instance`), should be applied only at root project (once), provides instance related tasks: `aemAwait`, `aemSetup`, `aemCreate` etc,
* package (`com.cognifide.aem.package`), should be applied to all projects that are composing CRX packages from *JCR content only*, provides CRX package related tasks: `aemCompose`, `aemDeploy` etc.
* bundle (`com.cognifide.aem.bundle`), should be applied to all projects that are composing CRX packages from both *OSGi bundle* being built and optionally *JCR content*, extends package plugin.

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

### Deploy CRX package(s) only to filtered group of instances:

When there are defined named AEM instances: `local-author`, `local-publish`, `integration-author` and `integration-publish`,
then it is available to deploy packages with taking into account: 

 * type of environment (local, integration, staging, etc)
 * type of AEM instance (author / publish)

```bash
gradlew aemDeploy -Paem.instance.name=integration-*
gradlew aemDeploy -Paem.instance.name=*-author
```

Default value of that instance name filter is `local-*`.

### Deploy CRX package(s) only to instances specified explicitly

Instance urls delimited by semicolon:

```bash
gradlew aemDeploy -Paem.instance.list=http://admin:admin@localhost:4502;http://admin:admin@localhost:4503
```

Alternative syntax - list delimited: instances by semicolon, instance properties by comma.

```bash
gradlew aemDeploy -Paem.instance.list=http://localhost:4502,admin,admin;http://localhost:4503,admin,admin
```

### Deploy only filtered dependent CRX package(s)

Filters with wildcards, comma delimited.

```bash
gradlew aemSatisfy -Paem.satisfy.group=hotfix-*,groovy-console
```

### Customize local AEM instances configuration

Plugin allows to override or provide extra files to local AEM instance installations.
This behavior is controlled by:

```groovy
aem {
    config {
        createPath = "${System.getProperty("user.home")}/.aem/${project.rootProject.name}"
        createFilesPath = project.rootProject.file("src/main/resources/local-instance")
        createFilesExpanded = [
          "**/*.properties", 
          "**/*.sh", 
          "**/*.bat", 
          "**/*.xml",
          "**/start",
          "**/stop"
      ]
    }
}
```

* Property *createPath* determines where AEM instance files will be extracted on local file system.
* Property *createFilesPath* determines project location that holds extra instance files that will override plugin defaults (start / stop scripts) and / or extracted AEM files.
* Property *createFilesExpandable* specifies which AEM instance files have an ability to use [expandable properties](#expandable-properties) inside.

In other words, to customize instance files just:

* Copy [default start / stop scripts](https://github.com/Cognifide/gradle-aem-plugin/tree/master/src/main/resources/com/cognifide/gradle/aem/local-instance) to project path controlled by *createFilesPath*
* Customize copied scripts.
* Provide other AEM files that need to be added or overridden.

### Check out and clean JCR content using filter at custom path
   
E.g for subproject `:content`:
   
```bash
gradlew :content:aemSync -Paem.checkout.filterPath=custom-filter.xml
gradlew :content:aemSync -Paem.checkout.filterPath=src/main/content/META-INF/vault/custom-filter.xml
gradlew :content:aemSync -Paem.checkout.filterPath=C:/aem/custom-filter.xml
```

### Check out and clean JCR content using filter roots specified explicitly
   
```bash
gradlew :content:aemSync -Paem.checkout.filterRoots=[/etc/tags/example,/content/dam/example]
```

### Assemble all-in-one CRX package(s)

Let's assume following project structure (with build file contents):

* build.gradle (project `:`)

```groovy
apply plugin: 'com.cognifide.aem.package'

aemCompose {
    includeProjects(':app:*')
    includeProjects(':content:*')
    includeProject(':migration')
}

```

When building via command `gradlew :build`, then the effect will be a CRX package with assembled JCR content and OSGi bundles from projects: `:app:core`, `:app:common`, `:content:init`, `:content:demo` and `:migration`.

* app/build.gradle  (project `:app`)

```groovy
apply plugin: 'com.cognifide.aem.package'

aemCompose {
    includeSubprojects()
}

```

When building via command `gradlew :app:build`, then the effect will be a CRX package with assembled JCR content and OSGi bundles from projects: `:app:core`, `:app:common` only.

* *app/core/build.gradle* (project `:app:core`, JCR content and OSGi bundle)
* *app/common/build.gradle* (project `:app:common`, JCR content and OSGi bundle)
* *content/init/build.gradle* (project `:content:init`, JCR content only)
* *content/demo/build.gradle* (project `:content:demo`, JCR content only)
* *migration/build.gradle* (project `:migration`, JCR content only)
* *test/integration/build.gradle* (project `:test:integration`, any source code)
* *test/functional/build.gradle* (project `:test:functional`, any source code)

Gradle AEM Plugin is configured in that way that project can have:
 
* JCR content
* source code to compile OSGi bundle
* both

By distinguishing `includeProject`, `includeBundle` or `includeContent` there is ability to create any assembly CRX package with content of any type without restructuring the project.
Only one must have rule to be kept while developing a multi-module project is that **all Vault filter roots of all projects must be exclusive**.
In general, they are most often exclusive, to avoid strange JCR installer behaviors, but sometimes exceptional [workspace filter](http://jackrabbit.apache.org/filevault/filter.html) rules are being applied like `mode="merge"` etc.

### Include additional OSGi bundle into CRX package

Simply use custom [configuration](https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html) named `aemInstall` in `dependencies` section.

In build script, instead using `compile 'group:name:version'` use `aemInstall 'group:name:version'`. As a result, artifact will be copied into CRX package being composed at path determined by property `config.bundlePath`.

For the reference, see [usage in AEM Multi-Project Example](https://github.com/Cognifide/gradle-aem-multi/blob/master/app/common/build.gradle).

### Embed JAR file into built OSGi bundle

Simply use custom [configuration](https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html) named `aemEmbed` in `dependencies` section.

In build script, instead using `compile 'group:name:version'` use `aemEmbed 'group:name:version'`. As a result, artifact will become a part of OSGi bundle being built. 

To be able to use classes provided by that dependency, it is recommended to specify packages in bundle manifest attribute named `Private-Package` or optionally `Export-Package`. 
For the reference, see [usage in AEM Multi-Project Example](https://github.com/Cognifide/gradle-aem-multi/blob/master/app/common/build.gradle).


### Skip installed package resolution by download name. 

```bash
gradlew aemInstall -Paem.package.skipDownloadName=false
```
Only matters when Vault properties file is customized then that property could be used to eliminate conflicts.

## Contributing

### Basics

* Work on fork of this repository - In the top-right corner of this page, click Fork.
* All your changes should be created from and merged into **develop** branch.
* If you want your changes to be included in upstream release Create Pull Request from your fork **develop** branch into upstream **develop** branch
    * Before you create Pull Request make sure that plugin is working with **AEM [Single|Multi]-Project Example** projects

### Testing local version of plugin

After you have done some changes make sure that local version plugin works with one of **AEM Project Example**:
* [AEM Single-Project Example](https://github.com/Cognifide/gradle-aem-single#quickstart) 
* [AEM Multi-Project Example](https://github.com/Cognifide/gradle-aem-multi#quickstart) 

by publishing your developed version of **Gradle AEM Plugin** to a local maven.


Clone into one of **AEM Project Example** projects.

Add local maven *mavenLocal()* on top of repositories in **build.gradle** file:

```groovy
pluginManagement {
    repositories {
        mavenLocal()
        jcenter()
        maven { url "https://plugins.gradle.org/m2/" }
        maven { url  "http://dl.bintray.com/cognifide/maven-public" }
        maven { url  "https://dl.bintray.com/neva-dev/maven-public" }
    }
    ...
}
```

Change version of *com.cognifide.gradle:aem-plugin* in **build.gradle** file:

```groovy
pluginManagement {
       resolutionStrategy {
        eachPlugin {
        ...
                useModule('com.cognifide.gradle:aem-plugin:4.0.x')
        ...
        }
    }
}
```
Go back into **Gradle AEM Plugin** project.

Update plugin version in build.gradle to the one you have configured in **AEM Project Example**.

```groovy
group 'com.cognifide.gradle'
version '4.0.x'
description = 'Gradle AEM Plugin'
defaultTasks = ['clean', 'build', 'publishToMavenLocal']
``` 

Build **Gradle AEM Plugin**

```bash
sh gradlew
```

by default it will run publish to local maven task.

Now **AEM Project Example** will use your local version of **Gradle AEM Plugin**.

### Debug

Execute **AEM Project Example** build with debug options (make sure it is using **local** version of **Gradle AEM Plugin**), it will suspend.

```bash
sh grdlew -Dorg.gradle.debug=true --no-daemon
```

Attach **Gradle AEM Plugin** debugger on port 5005.

Suspension will be released and build should stop at breakpoint.

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

## License

**Gradle AEM Plugin** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)
