![Cognifide logo](docs/cognifide-logo.png)

[![Gradle Status](https://gradleupdate.appspot.com/Cognifide/gradle-aem-plugin/status.svg)](https://gradleupdate.appspot.com/Cognifide/gradle-aem-plugin/status)
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/Cognifide/gradle-aem-plugin.svg?label=License)](http://www.apache.org/licenses/)

# Gradle AEM Plugin

<br>
<p align="center">
  <img src="docs/logo.png" alt="Gradle AEM Plugin Logo"/>
</p>
<br>

Currently there is no popular way to build applications for AEM using Gradle build system. This project contains brand new Gradle plugin to assemble CRX package and deploy it on instance(s).

Incremental build which takes seconds, not minutes. Developer who does not loose focus between build time gaps. Extend freely your build system directly in project. 

AEM developer - it's time to meet Gradle! You liked or used plugin? Don't forget to **star this project** on GitHub :)

## Features

* Composing CRX package from multiple JCR content roots, bundles.
* Automated all-in-one CRX packages generation (assemblies).
* Easy multi-deployment with instance groups.
* Automated dependent packages installation from local and remote sources.
* Smart Vault files generation (combining defaults with overiddables).
* Checking out and cleaning JCR content from running AEM instance.
* OSGi Manifest customization by official [osgi](https://docs.gradle.org/current/userguide/osgi_plugin.html) plugin or feature rich [org.dm.bundle](https://github.com/TomDmitriev/gradle-bundle-plugin) plugin.
* OSGi Declarative Services annotations support (instead of SCR, [see docs](http://blogs.adobe.com/experiencedelivers/experience-management/osgi/using-osgi-annotations-aem6-2/)).

## Requirements

* Java >= 8, but target software can be compiled to older Java.
* Gradle  >= 3.5.

## Configuration

Recommended way to start using Gradle AEM Plugin is to clone and customize [example project](https://github.com/Cognifide/gradle-aem-example).
General configuration options are listed [here](src/main/kotlin/com/cognifide/gradle/aem/AemConfig.kt).

### Plugin setup

Released versions of plugin are available on [Bintray](https://bintray.com/cognifide/maven-public/gradle-aem-plugin), 
so that this repository need to be included in *buildscript* section.


#### Minimal:

```
buildscript {
    repositories {
        maven { url  "http://dl.bintray.com/cognifide/maven-public" }
    }
    
    dependencies {
        classpath 'com.cognifide.gradle:aem-plugin:1.2.+'
    }
}

apply plugin: 'com.cognifide.aem'

build.dependsOn aemCompose
```

Building and deploying to AEM via command: `gradle build aemDeploy`.

#### Extra:

```
apply plugin: 'kotlin' // 'java' or whatever you like to compile bundle

defaultTasks = ['appDeploy']

aem {
    config {
        contentPath = "src/main/content"
        instance("http://localhost:4502", "admin", "admin", "local-author")
        // instance("http://localhost:4503", "admin", "admin", "local-publish")
    }
}

aemSatisfy {
    // local("pkg/vanityurls-components-1.0.2.zip")
    download("https://github.com/Cognifide/APM/releases/download/cqsm-3.0.0/apm-3.0.0.zip")
}

task appDeploy(dependsOn: [build, aemDeploy])
```

Preinstalling dependent packages on AEM via command: `gradle aemSatisfy`.

Building and deploying to AEM via command: `gradle appDeploy` or just `gradle`.


Instances configuration can be omitted, then *http://localhost:4502* and *http://localhost:4503* will be used by default.
Content path can also be skipped, because value above is also default. This is only an example how to customize particular [values](src/main/kotlin/com/cognifide/gradle/aem/AemConfig.kt).

For multi project build configuration, see [example project](https://github.com/Cognifide/gradle-aem-example).

### Tasks

* `aemCompose` - Compose CRX package from JCR content and bundles. Available methods:
    * `includeProject(projectPath: String)`, includes both bundles and JCR content from another project, example: `includeProject ':core'`.
    * `includeContent(projectPath: String)`, includes only JCR content, example: `includeContent ':design'`.
    * `includeBundles(projectPath: String)`, includes only bundles, example: `includeBundles ':common'`.
    * `includeBundlesAtRunMode(projectPath: String, runMode: String)`, as above, useful when bundles need to be installed only on specific type of instance.
    * `includeProjects(pathPrefix: String)`, includes both bundles and JCR content from all AEM projects (excluding itself) in which project path is matching specified filter. Vault filter roots will be automatically merged and available in property `${filterRoots}` in *filter.xml* file. Useful for building assemblies (all-in-one packages).
    * `includeSubprojects()`, alias for method above: `includeProjects("${project.path}:*")`.
    * all inherited from [ZIP task](https://docs.gradle.org/3.5/dsl/org.gradle.api.tasks.bundling.Zip.html).
* `aemUpload` - Upload composed CRX package into AEM instance(s).
* `aemInstall` - Install uploaded CRX package on AEM instance(s).
* `aemActivate` - Replicate installed CRX package to other AEM instance(s).
* `aemDeploy` - Upload & install CRX package into AEM instance(s). Primary, recommended form of deployment. Optimized version of `aemUpload aemInstall`.
* `aemDistribute` - Upload, install & activate CRX package into AEM instances(s). Secondary form of deployment. Optimized version of `aemUpload aemInstall aemActivate -Paem.deploy.instance.group=*-author`.
* `aemSatisfy` - Upload & install dependent CRX package(s) before deployment. Available methods:
    * `local(path: String)`, use CRX package from local file system.
    * `local(file: File)`, same as above, but file can be even located outside the project.
    * `download(url: String)`, use CRX package that will be downloaded from specified URL to local temporary directory.
    * `downloadBasicAuth(url: String, user = "admin", password = "admin")`, as above, but with Basic Auth support.
    * `group(name: String, configurer: () -> Unit)`, useful for declaring group of packages to be installed only on demand (just use methods above in closure).
* `aemCheckout` - Check out JCR content from running AEM author instance to local content path.
* `aemClean` - Clean checked out JCR content.
* `aemSync` - Check out then clean JCR content.

### Parameters

* Deploying only to filtered group of instances (filters with wildcards, comma delimited):

```
-Paem.deploy.instance.group=integration-*
-Paem.deploy.instance.group=*-author
```
   
* Deploying only to instances specified explicitly: 

```
-Paem.deploy.instance.list=http://localhost:4502,admin,admin;http://localhost:4503,admin,admin
```

* Satisfying only filtered group of packages (filters with wildcards, comma delimited):

```
-Paem.deploy.satisfy.group=tools 
```

* Skipping installed package resolution by download name (eliminating conflicts / only matters when Vault properties file is customized): 

```
-Paem.deploy.skipDownloadName=true
```

* Checking out JCR content using filter at custom path:

```
-Paem.vlt.checkout.filterPath=src/main/content/META-INF/vault/custom-filter.xml
```

### Expandable properties

By default, plugin is configured that in all XML files, properties can be injected:

```
aem {
    vaultFilesExpanded = ["*.xml"]
    vaultExpandProperties = [:]
}
```

This feature is specially useful to generate valid *META-INF/properties.xml* file.
What is more, there are predefined variables that also can be used:

* `rootProject` - project with directory in which *settings.gradle* is located.
* `project` - current project.
* `config` - [AEM configuration](src/main/kotlin/com/cognifide/gradle/aem/AemConfig.kt).
* `created` - current date in ISO8601 format.
* `buildCount` - number to be used as CRX package build count (`config.buildDate` in format `yDDmmssSSS`).
* `filterRoots` - after using method `includeContent` of `aemCompose` task, all Vault filter roots are being gathered. This variable contains all these XML tags concatenated especially useful for building assemblies. If no projects will be included, then this variable will contain a single filter root with bundle install path to be able to deploy auto-generated package with JAR file only.

## License

**Gradle AEM Plugin** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)


