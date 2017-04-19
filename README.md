![Cognifide logo](http://cognifide.github.io/images/cognifide-logo.png)

# Gradle AEM Plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/neva-dev/felix-search-webconsole-plugin.svg?label=License)](http://www.apache.org/licenses/)

## About

Currently there is no popular way to build applications for AEM using Gradle build system. This project contains brand new Gradle plugin to assemble CRX package and deploy it on instance.

Incremental build which takes seconds, not minutes. Developer who does not loose focus between build time gaps. Extend freely your build system directly in project. 

AEM developer - it's time to meet Gradle!

## Features

* Composing CRX package from multiple content roots, bundles.
* Easy multi-deployment with instance groups.
* Automated dependant packages installation from local and remote sources.
* Jar embedding into bundles.

## Tasks

* `aemCompose` - Compose CRX package from JCR content and bundles.
* `aemUpload` - Upload composed CRX package into AEM instance(s).
* `aemInstall` - Install uploaded CRX package on AEM instance(s).
* `aemActivate` - Replicate installed CRX package to other AEM instance(s).
* `aemDeploy` - Upload & install CRX package into AEM instance(s). Primary, recommended for of deployment. Optimized version of `aemUpload aemInstall`.
* `aemDistribute` - Upload, install & activate CRX package into AEM instances(s). Secondary form of deployment. Optimized version of `aemUpload aemInstall aemActivate -Paem.deploy.group=*-author`.
* `aemSatisfy` - Upload & install dependant CRX package(s) before deployment.

## Configuration

### Tasks

```
plugins.withId 'cognifide.aem', {

    // Global configuration
    
    aem {
        config {
            contentPath = "src/main/content"
            instance("http://localhost:4502", "admin", "admin", "local-author")
            instance("http://localhost:4503", "admin", "admin", "local-publish")
        }
    }

    // Project specific configuration
    
    aemCompose {
        config {
            contentPath = "src/main/aem"
        }
    }
    
    // Other task specific configurations
    
    // ...
}

```

### Command line:

* Deploying only to filtered group of instances: `-Paem.deploy.group=author`, default: `*`.
* Skipping installed package resolution by download name (eliminating conflicts): `-Paem.deploy.skipDownloadName=true`, default: `false`.

## License

**Gradle AEM Plugin** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)


