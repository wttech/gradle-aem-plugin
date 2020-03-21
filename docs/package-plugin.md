[![Cognifide logo](cognifide-logo.png)](http://cognifide.com)

<p>
  <img src="logo.png" alt="Gradle AEM Plugin"/>
</p>

# Package plugin

  * [About](#about)
  * [Task packageCompose](#task-packagecompose)
     * [CRX package default configuration](#crx-package-default-configuration)
     * [CRX package naming](#crx-package-naming)
     * [CRX package validation](#crx-package-validation)
     * [Including additional OSGi bundle into CRX package](#including-additional-osgi-bundle-into-crx-package)
     * [Nesting CRX packages](#nesting-crx-packages)
     * [Assembling packages (merging all-in-one)](#assembling-packages-merging-all-in-one)
     * [Expandable properties](#expandable-properties)
  * [Task packagePrepare](#task-packageprepare)
  * [Task packageValidate](#task-packagevalidate)
  * [Task packageDeploy](#task-packagedeploy)
     * [Deploying only to desired instances](#deploying-only-to-desired-instances)
     * [Deploying options](#deploying-options)
  * [Task packageUpload](#task-packageupload)
  * [Task packageDelete](#task-packagedelete)
  * [Task packageInstall](#task-packageinstall)
  * [Task packageUninstall](#task-packageuninstall)
  * [Task packagePurge](#task-packagepurge)
  * [Task packageActivate](#task-packageactivate)
  * [Known issues](#known-issues)
     * [Caching task packageCompose](#caching-task-packagecompose)

## About

Main responsibility of this plugin is to build CRX/AEM package.

Provides CRX package related tasks: `packageCompose`, `packageDeploy`, `packageActivate`, `packagePurge` etc.

Should be applied to all projects that are composing CRX packages from *JCR content only*.

To apply plugin use snippet:

```kotlin
plugins {
    id("com.cognifide.aem.package")
}
```

This plugin implicitly applies also [Common Plugin](common-plugin.md).

## Task `packageCompose`

Compose CRX package from JCR content and bundles. 

Inherits from task [ZIP](https://docs.gradle.org/3.5/dsl/org.gradle.api.tasks.bundling.Zip.html).

### CRX package default configuration

```kotlin
aem {
    tasks {
        packageCompose {
            archiveBaseName.set(this@aem.baseName)
            duplicatesStrategy = DuplicatesStrategy.WARN
            contentDir.set(packageOptions.contentDir)
            metaDir.set(contentDir.dir("META-INF"))
            bundlePath.set(packageOptions.installPath)
            nestedPath.set(packageOptions.storagePath)
            vaultDefinition {
                properties.set(mapOf(
                    "acHandling" to "merge_preserve",
                    "requiresRoot" to false
                ))
                name.set(archiveBaseName)
                group.set(project.grouop)
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

### CRX package naming

ZIP file name and metadata (Vault properties) of composed CRX package are set by convention - they are derived from project names and Gradle `project.group` and `project.version` properties. 
However, only if it is required (adapting the project to fit convention is more recommended approach), following snippet might be useful, to know how to customize particular values:

```kotlin
aem {
    tasks {
        packageCompose {
            // Controlling ZIP file name by properties coming from inherited Gradle ZIP task: https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Zip.html
            archiveBaseName.set("my-package")
    
            // Controlling values visible in CRX Package Manager
            vaultDefinition { 
                properties.set(mapOf(
                    "acHandling" to "merge_preserve",
                    "requiresRoot" to false
                ))
                name.set(archiveBaseName)
                group.set(project.group)
            }
        }
    }
}
```

### CRX package validation

Built package is validated using [OakPAL tool](https://github.com/adamcin/oakpal).
It helps preventing situation when CRX Package Manager reports message like *Package installed with errors*.
Also gives immediate feedback for mistakes taken when using copy-pasting technique, 
forgetting about using entities ('&' instead of '\&amp;), missing XML namespaces and much more.

By default, **nothing need to be configured** so that [default plan](https://github.com/Cognifide/gradle-aem-plugin/blob/master/src/main/resources/com/cognifide/gradle/aem/package/OAKPAL_OPEAR/plan.json) will be in use.
However, more detailed checks could be provided by configuring base artifact called Opear File containing OakPAL checks.

It could be done via following snippet ([ACS AEM Commons OakPAL Checks](https://github.com/Adobe-Consulting-Services/acs-aem-commons/tree/master/oakpal-checks) as example):

```kotlin
aem {
    `package` {
        validator {
            base("com.adobe.acs:acs-aem-commons-oakpal-checks:4.3.4")
        }
    }
}
```

To use custom checks, only left thing is to choose them in custom plan.
Simply create files *plan-compose.json* *plan-satisfy.json* or only *plan.json* under path [src/main/resources/com/cognifide/gradle/aem/package/OAKPAL_OPEAR](https://github.com/Cognifide/gradle-aem-multi/blob/master/app/aem/common/package/OAKPAL_OPEAR)

Sample plan content:

```json
{
  "checklists": [
    "net.adamcin.oakpal.core/basic",
    "acs-commons-integrators",
    "content-class-aem65"
  ],
  "installHookPolicy": "SKIP",
  "checks": [
    {
      "name": "basic/subpackages",
      "config": {
        "denyAll": false
      }
    },
    {
      "name": "basic/acHandling",
      "config": {
        "levelSet": "no_unsafe"
      }
    }
  ]
}
```

See [plans documentation](http://adamcin.net/oakpal/the-oakpal-plan.html) and [examples](https://github.com/adamcin/oakpal/blob/master/core/src/test/resources/OakpalPlanTest/fullPlan.json) provided by OakPAL tool.

There could be many plan files created. To validate CRX package using different plan than default one, specify property `-package.validator.planName=<file>`.

Notice that running OakPAL requires to have included in CRX package up-to-date node type definitions coming from AEM instance.
Such definitions could be manually downloaded using CRXDE Lite interface (*Tools / Export Node Type*) and put inside CRX package.
After installing some dependent CRX packages, the list of exported node types may change.

To keep it up-to-date, plugin is synchronizing node types from one of available AEM instances automatically.
Synchronized file containing node types, later used when building CRX packages is placed at path [*[aem/]gradle/package/nodetypes.sync.cnd*](https://github.com/Cognifide/gradle-aem-multi/blob/master/aem/gradle/package/nodetypes.sync.cnd). 
Remember to save this file in VCS, so that CRX package validation will not fail on e.g CI server where AEM instance could be not available.

To configure node types synchronization behavior, use property `package.nodeTypesSync=<option>`. Available options: *preserve_auto* (default, sync only if file *nodetypes.sync.cnd* does not exist), *always*, *auto*, *fallback*, *preserve_fallback*, *never*.

Package validation report is saved at path relative to project building CRX package: *build/aem/packageCompose/OAKPAL_OPEAR/report.json*.

### Including additional OSGi bundle into CRX package

Use dedicated task method named `fromJar`, for example:

```kotlin
aem {
    tasks {
        packageCompose {
            installBundle("com.github.mickleroy:aem-sass-compiler:1.0.1")
        }
    }
}
```

For reference, see usage above in [AEM Multi-Project Example](https://github.com/Cognifide/gradle-aem-multi/blob/master/aem/common/build.gradle.kts).

### Nesting CRX packages

Use dedicated task method named `fromZip`, For example:

```kotlin
aem {
    tasks {
        packageCompose {
            nestPackage("com.adobe.cq:core.wcm.components.all:2.4.0")
            nestPackage("com.adobe.cq:core.wcm.components.examples:2.4.0")
        }
    }
}
```

### Assembling packages (merging all-in-one)

Let's assume following project structure:

* *aem/build.gradle.kts* (project `:aem`, no source files at all)
* *aem/common/build.gradle.kts*  (project `:aem:common`, JCR content and OSGi bundle)
* *aem/sites/build.gradle.kts*  (project `:aem:sites`, JCR content and OSGi bundle)
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
            mergePackageProject(":aem:common")
            mergePackageProject(":aem:sites")
            mergePackageProject(":aem:site.live")
            mergePackageProject(":aem:site.demo")
        }
    }    
}
```

When building via command `gradlew :aem:build`, then the effect will be a CRX package with assembled JCR content and OSGi bundles from projects: `:aem:sites`, `:aem:common`, `:aem:site.live`, `:aem:site.demo`.

Gradle AEM Plugin is configured in a way that project can have:
 
* JCR content,
* source code to compile OSGi bundle,
* both.

By mixing usages of methods `nestPackage*`, `installBundle*` or `mergePackage*` there is ability to create any assembly CRX package with content of any type without restructuring the project.

When using `installBundle` there is an ability to pass lambda to customize options like bundle run mode.

However, one rule must be kept while developing a multi-module project: **all Vault filter roots of all projects must be exclusive**. In general, they are most often exclusive, to avoid strange JCR installer behaviors, but sometimes exceptional [workspace filter](http://jackrabbit.apache.org/filevault/filter.html) rules are being applied like `mode="merge"` etc.

### Expandable properties

In exactly the same way as it works for instance files, properties can be expanded inside metadata files of package being composed.

Related configuration:

```kotlin
aem {
    tasks {
        packageCompose {
            fileFilter {
                expandProperties.set(mapOf(
                    "organization" to "Company"
                ))
                expandFiles.set(listOf(
                    "**/META-INF/*.xml",
                    "**/META-INF/*.MF",
                    "**/META-INF/*.cnd"
                ))
            }
        }
    }
}
```

Predefined expandable properties:

* `aem` - [AemExtension](src/main/kotlin/com/cognifide/gradle/aem/AemExtension.kt) object,
* `definition` - [VaultDefinition](src/main/kotlin/com/cognifide/gradle/aem/common/pkg/vault/VaultDefinition.kt) object,
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
    {% for e in definition.properties %}<entry key="{{e.key}}">{{e.value | raw}}</entry>
    {% endfor %}
</properties>
```

Also file *nodetypes.cnd* is dynamically expanded from [template](src/main/resources/com/cognifide/gradle/aem/package/META-INF/vault/nodetypes.cnd) to generate file containing all node types from all sub packages being merged into assembly package.

Each JAR file in separate *hooks* directory will be combined into single directory when creating assembly package.

## Task `packagePrepare`

Processes CRX package metadata - combines default files provided by plugin itself with overridden ones.
Also responsible for synchronizing Vault node types consumed later by [CRX package validation](#crx-package-validation) in the end of [compose task](#task-packagecompose).
Covers extracted logic being initially a part of compose task. Reason of separation is effectively better Gradle caching.
 
## Task `packageValidate`

Validates composed CRX package.

For setting common options see section [CRX package validation](#crx-package-validation). 

For setting project/package specific options use snippet below:

```kotlin
tasks {
    packageValidate {
        validator {
            enabled.set(true)
            verbose.set(true)
            severity("MAJOR")
            planName.set("plan.json")
            jcrPrivileges.set(listOf("crx:replicate"))
            cndFiles.from(packageOptions.configDir.file("nodetypes.cnd"))
        }
    }
}
```
 
## Task `packageDeploy` 

Upload & install CRX package into AEM instance(s). 

Recommended form of deployment. Optimized version of `packageUpload packageInstall`.

### Deploying only to desired instances

Simply follow general rules about [instance filtering](common-plugin.md#instance-filtering).

### Deploying options

Add any of below command line parameters to customize CRX package deployment behavior:

* `-Ppackage.deploy.awaited=false` - disable stability & health checks after deploying CRX package.
* `-Ppackage.deploy.distributed=true` - use alternative form of deployment. At first, deploys CRX package to author instances, then triggers replication of CRX package so that it will be installed also on publish instances.
* `-Ppackage.deploy.uploadForce=false` - disable force installation (by default even unchanged CRX package is forced to be reinstalled)
* `-Ppackage.deploy.installRecursive=false` - disable automatic installation of subpackages located inside CRX package being deployed.  
* `-Ppackage.deploy.uploadRetry=n` - customize number of retries being performed after failed CRX package upload.
* `-Ppackage.deploy.installRetry=n` - customize number of retries being performed after failed CRX package install.
* `-Ppackage.deploy.workflowToggle=[id1=true,id2=false,...]` - temporarily enable or disable AEM workflows during deployment e.g when CRX package contains generated DAM asset renditions so that regeneration could be avoided and deploy time reduced. For example: `-Ppackage.deploy.workflowToggle=[dam_asset=false]`. Workflow ID *dam_asset* is a shorthand alias for all workflows related with DAM asset processing.

## Task `packageUpload`

Upload composed CRX package into AEM instance(s).

## Task `packageDelete`

Delete uploaded CRX package from AEM instance(s).

## Task `packageInstall`

Install uploaded CRX package on AEM instance(s).

## Task `packageUninstall`

Uninstall uploaded CRX package on AEM instance(s).

To prevent data loss, this unsafe task execution must be confirmed by parameter `-Pforce`.

## Task `packagePurge` 

Fail-safe combination of `packageUninstall` and `packageDelete`.

To prevent data loss, this unsafe task execution must be confirmed by parameter `-Pforce`.

## Task `packageActivate` 

Replicate installed CRX package to other AEM instance(s).

## Known issues

### Caching task `packageCompose`

Expandable properties with dynamically calculated value (unique per build) like `created` and `buildCount` are not used by default generated properties file intentionally, 
because such usages will effectively forbid caching `packageCompose` task and it will be never `UP-TO-DATE`.
