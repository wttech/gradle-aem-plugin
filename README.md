![Cognifide logo](doc/cognifide-logo.png)

[![Gradle Status](https://gradleupdate.appspot.com/Cognifide/gradle-aem-plugin/status.svg)](https://gradleupdate.appspot.com/Cognifide/gradle-aem-plugin/status)
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/Cognifide/gradle-aem-plugin.svg?label=License)](http://www.apache.org/licenses/)

# Gradle AEM Plugin

<br>
<p align="center">
  <img src="doc/logo.png" alt="Gradle AEM Plugin Logo"/>
</p>
<br>

Currently there is no popular way to build applications for AEM using Gradle build system. This project contains brand new Gradle plugin to assemble CRX package and deploy it on instance(s).

Incremental build which takes seconds, not minutes. Developer who does not loose focus between build time gaps. Extend freely your build system directly in project. 

AEM developer - it's time to meet Gradle!

## Features

* Composing CRX package from multiple content roots, bundles.
* Easy multi-deployment with instance groups.
* Service component annotations processing (SCR).
* OSGi manifest customization by official [osgi](https://docs.gradle.org/current/userguide/osgi_plugin.html) plugin or feature rich [org.dm.bundle](https://github.com/TomDmitriev/gradle-bundle-plugin) plugin.
* Automated dependant packages installation from local and remote sources.
* Smart Vault files generation (combining defaults with overiddables).
* Checking out and cleaning JCR content from running AEM instance.

## Requirements

* Java >= 8, but target software can be compiled to older Java.
* Gradle  >= 3.5.

## Configuration

Recommended way to start using Gradle AEM Plugin is to clone and customize [example project](https://github.com/Cognifide/gradle-aem-example).
All configuration options are listed [here](src/main/kotlin/com/cognifide/gradle/aem/AemConfig.kt).

Example configuration listed below assumes building project by single command `gradle contentDeploy` or just `gradle`.

### Root project (shared)

```
defaultTasks = ['contentDeploy']

plugins.withId 'cognifide.aem', {

    aem {
        config {
            contentPath = "src/main/content"
            instance("http://localhost:4502", "admin", "admin", "local-author")
            // instance("http://localhost:4503", "admin", "admin", "local-publish")
        }
    }

}

```

Instances configuration can be even omitted, then *http://localhost:4502* and *http://localhost:4503* will be used by default.
Content path can also be skipped, because value above is also default. This is only an example how to customize particular values.


### Sub project (specific)

```
defaultTasks = ['contentDeploy']

apply plugin: 'cognifide.aem'

aem {
    config {
        contentPath = "src/main/aem"
    }
}

aemSatisfy {
    // local("pkg/vanityurls-components-1.0.2.zip")
    download("https://github.com/Cognifide/APM/releases/download/cqsm-3.0.0/apm-3.0.0.zip")
}

aemCompose {
    includeProject ':example.bundle'
}

build.dependsOn aemCompose
task contentDeploy(dependsOn: [clean, build, aemDeploy])

```

Snippet above demonstrates customizations valid only for specific project.

### Tasks

* `aemCompose` - Compose CRX package from JCR content and bundles. Available methods:
    * `includeProject(projectName: String)`, includes both bundles and JCR content from another project, example: `includeProject ':example.bundle'`.
    * `includeContent(projectName: String)`, includes only JCR content, example: `includeContent ':example.design'`.
    * `includeBundles(projectName: String)`, includes only bundles, example: `includeBundles ':example.auth'`.
    * all inherited from [ZIP task](https://docs.gradle.org/3.5/dsl/org.gradle.api.tasks.bundling.Zip.html).
* `aemUpload` - Upload composed CRX package into AEM instance(s).
* `aemInstall` - Install uploaded CRX package on AEM instance(s).
* `aemActivate` - Replicate installed CRX package to other AEM instance(s).
* `aemDeploy` - Upload & install CRX package into AEM instance(s). Primary, recommended form of deployment. Optimized version of `aemUpload aemInstall`.
* `aemDistribute` - Upload, install & activate CRX package into AEM instances(s). Secondary form of deployment. Optimized version of `aemUpload aemInstall aemActivate -Paem.deploy.instance.group=*-author`.
* `aemSatisfy` - Upload & install dependant CRX package(s) before deployment. Available methods:
    * `local(path: String)`, use CRX package from local file system.
    * `download(url: String)`, use CRX package that will be downloaded from specified URL to local temporary directory.
    * `downloadBasicAuth(url: String, user = "admin", password = "admin")`, as above, but with Basic Auth support.
* `aemCheckout` - Check out JCR content from running AEM author instance to local content path.
* `aemClean` - Clean checked out JCR content.
* `aemSync` - Check out then clean JCR content.

### Parameters

* Deploying only to filtered group of instances

```
-Paem.deploy.instance.group=integration-*
-Paem.deploy.instance.group=*-author
```
   
* Deploying only to instances specified explicitly: 

```
-Paem.deploy.instance.list=http://localhost:4502,admin,admin;http://localhost:4503,admin,admin
```

* Skipping installed package resolution by download name (eliminating conflicts / only matters when Vault properties file is customized): 

```
-Paem.deploy.skipDownloadName=true
```

## License

**Gradle AEM Plugin** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)


