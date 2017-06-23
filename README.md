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
* Embedded Vault tool for checking out and cleaning JCR content from running AEM instance.
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
        classpath 'com.cognifide.gradle:aem-plugin:1.3.+'
    }
}

apply plugin: 'com.cognifide.aem'
```

Building and deploying to AEM via command: `gradle aemRootDeploy` or `gradle build aemDeploy`.

#### Extra:

```
apply plugin: 'kotlin' // 'java' or whatever you like to compile bundle

defaultTasks = ['aemRootDeploy']

aem {
    config {
        contentPath = project.file("src/main/content")
        instance("http://localhost:4502", "admin", "admin", "local-author")
        // instance("http://localhost:4503", "admin", "admin", "local-publish")
    }
}

aemSatisfy {
    // local("pkg/vanityurls-components-1.0.2.zip")
    download("https://github.com/Cognifide/APM/releases/download/cqsm-3.0.0/apm-3.0.0.zip")
}

```

Preinstalling dependent packages on AEM via command: `gradle aemSatisfy`.

Building and deploying to AEM via most simple command: `gradle`.

Instances configuration can be omitted, then *http://localhost:4502* and *http://localhost:4503* will be used by default.
Content path can also be skipped, because value above is also default. This is only an example how to customize particular [values](src/main/kotlin/com/cognifide/gradle/aem/AemConfig.kt).

For multi project build configuration, see [example project](https://github.com/Cognifide/gradle-aem-example).

### Tasks

* `aemCompose` - Compose CRX package from JCR content and bundles. Available methods:
    * `includeProject(projectPath: String)`, includes both bundles and JCR content from another project, example: `includeProject ':core'`.
    * `includeContent(projectPath: String)`, includes only JCR content, example: `includeContent ':design'`.
    * `includeBundles(projectPath: String)`, includes only bundles, example: `includeBundles ':common'`.
    * `includeBundles(projectPath: String, installPath: String)`, includes only bundles at custom install path, example: `includeBundles(':common', '/apps/my-app/install')`.
    * `includeBundlesAtRunMode(projectPath: String, runMode: String)`, as above, useful when bundles need to be installed only on specific type of instance.
    * `includeProjects(pathPrefix: String)`, includes both bundles and JCR content from all AEM projects (excluding itself) in which project path is matching specified filter. Vault filter roots will be automatically merged and available in property `${filterRoots}` in *filter.xml* file. Useful for building assemblies (all-in-one packages).
    * `includeSubprojects()`, alias for method above: `includeProjects("${project.path}:*")`.
    * all inherited from [ZIP task](https://docs.gradle.org/3.5/dsl/org.gradle.api.tasks.bundling.Zip.html).
* `aemUpload` - Upload composed CRX package into AEM instance(s).
* `aemInstall` - Install uploaded CRX package on AEM instance(s).
* `aemActivate` - Replicate installed CRX package to other AEM instance(s).
* `aemDeploy` - Upload & install CRX package into AEM instance(s). Primary, recommended form of deployment. Optimized version of `aemUpload aemInstall`.
* `aemDistribute` - Upload, install & activate CRX package into AEM instances(s). Secondary form of deployment. Optimized version of `aemUpload aemInstall aemActivate -Paem.deploy.instance.name=*-author`.
* `aemSatisfy` - Upload & install dependent CRX package(s) before deployment. Available methods:
    * `local(path: String)`, use CRX package from local file system.
    * `local(file: File)`, same as above, but file can be even located outside the project.
    * `download(url: String)`, use CRX package that will be downloaded from specified URL to local temporary directory.
    * `downloadBasicAuth(url: String, user = "admin", password = "admin")`, as above, but with Basic Auth support.
    * `group(name: String, configurer: () -> Unit)`, useful for declaring group of packages to be installed only on demand (just use methods above in closure).
* `aemCheckout` - Check out JCR content from running AEM author instance to local content path.
* `aemClean` - Clean checked out JCR content.
* `aemSync` - Check out then clean JCR content.
* `aemVlt` - Execute any Vault command. See parameters section for more details.

### Task rules

* `aem<ProjectPath>Deploy` - Build CRX package and deploy it to AEM instance(s). For root project use reserved `aemRootDeploy`. It is recommended to include appropriate deploy task name in [default tasks](https://docs.gradle.org/current/userguide/tutorial_using_tasks.html#sec:default_tasks) of project. For instance, to deploy project at path `:app:design` use task named `aemAppDesignDeploy`.

### Parameters

* Deploying only to filtered group of instances (filters with wildcards, comma delimited):

```
gradle aemDeploy -Paem.deploy.instance.name=integration-*
gradle aemDeploy -Paem.deploy.instance.name=*-author
```
   
* Deploying only to instances specified explicitly: 

```
gradle aemDeploy -Paem.deploy.instance.list=http://localhost:4502,admin,admin;http://localhost:4503,admin,admin
```

* Satisfying only filtered group of packages (filters with wildcards, comma delimited):

```
gradle aemSatisfy -Paem.deploy.satisfy.group=hotfix-*,groovy-console
```

* Checking out JCR content using filter at custom path (for subproject *content*):

```
gradle :content:aemCheckout -Paem.vlt.filter=src/main/content/META-INF/vault/custom-filter.xml
```

* Executing any Vault command at custom working directory (for subproject *content*):

```
gradle :content:aemVlt -Paem.vlt.command='checkout --force -f ${filter} ${instance.url}' 
```

Task `aemCheckout` is just an straightforward alias for above command. 
It is allowed to execute any command listed in [VLT Tool documentation](https://docs.adobe.com/docs/en/aem/6-2/develop/dev-tools/ht-vlttool.html).
Gradle requires to have working directory with file *build.gradle* in it, but Vault tool can work at any directory under *jcr_root*. To change working directory for Vault, use property `aem.vlt.path` which is relative path to be appended to *jcr_root* for project task being currently executed.

* Skipping installed package resolution by download name (eliminating conflicts / only matters when Vault properties file is customized): 

```
gradle aemInstall -Paem.deploy.skipDownloadName=true
```

### Expandable properties

By default, plugin is configured that in all XML files located under path *META-INF/vault*,properties can be injected using syntax: `${property}`.

Related configuration:

```
aem {
    fileProperties = [
        "organization": "My Company"
    ]
    vaultFilesExpanded = ["*.xml"]
}
```

This feature is specially useful to generate valid *META-INF/properties.xml* file.

Predefined properties:
* `rootProject` - project with directory in which *settings.gradle* is located.
* `project` - current project.
* `config` - [AEM configuration](src/main/kotlin/com/cognifide/gradle/aem/AemConfig.kt).
* `created` - current date in ISO8601 format.
* `buildCount` - number to be used as CRX package build count (`config.buildDate` in format `yDDmmssSSS`).

Task specific:
* `aemCompose` - properties which are being dynamically calculated basing on content actually included into package.
   * `filterRoots` - after using method `includeContent` of `aemCompose` task, all Vault filter roots are being gathered. This property contains all these XML tags concatenated especially useful for building assemblies. If no projects will be included, then this variable will contain a single filter root with bundle path to be able to deploy auto-generated package with JAR file only.
* `aemVlt` - properties are being injected to command specified in `aem.vlt.command` property. Following properties are being used internally also by `aemCheckout`.
   * `instance` - instance used to communicate with while performing Vault commands. Determined by (order take precedence): properties `aem.vlt.instance`, `aem.deploy.instance.list`, `aem.deploy.instance.name` and as fallback first instance which name matches filter `*-author`.
   * `filter` - path to Vault workspace filter file  *META-INF/vault/filter.xml*. Determined by (order take precedence): property: `aem.vlt.filter`, configuration `vaultFilterPath`.

## Known issues

### Vault tasks parallelism

Vault tool current working directory cannot be easily configured, because of its API. AEM plugin is temporarily changing current working directory for Vault, then returning it back to original value.
In case of that workaround, Vault tasks should not be run in parallel (by separated daemon processed / JVM synchronization bypassed), because of potential unpredictable behavior.

## License

**Gradle AEM Plugin** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)


