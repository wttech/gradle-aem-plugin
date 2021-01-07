![Cognifide logo](cognifide-logo.png)

![Gradle AEM Plugin](logo.png)

# Package plugin

*   [About](#about)
*   [Task packageCompose](#task-packagecompose)
    *   [CRX package default configuration](#crx-package-default-configuration)
    *   [CRX package naming](#crx-package-naming)
    *   [CRX package validation](#crx-package-validation)
    *   [Including additional OSGi bundle into the CRX package](#including-additional-osgi-bundle-into-crx-package)
    *   [Nesting CRX packages](#nesting-crx-packages)
    *   [Assembling packages (merging all-in-one)](#assembling-packages-merging-all-in-one)
    *   [Expandable properties](#expandable-properties)
    *   [Publishing packages](#publishing-packages)
*   [Task packagePrepare](#task-packageprepare)
*   [Task packageValidate](#task-packagevalidate)
*   [Task packageDeploy](#task-packagedeploy)
    *   [Deploying only to desired instances](#deploying-only-to-desired-instances)
    *   [Deploying options](#deploying-options)
*   [Task packageUpload](#task-packageupload)
*   [Task packageDelete](#task-packagedelete)
*   [Task packageInstall](#task-packageinstall)
*   [Task packageUninstall](#task-packageuninstall)
*   [Task packagePurge](#task-packagepurge)
*   [Task packageActivate](#task-packageactivate)
*   [Known issues](#known-issues)
    *   [Caching task packageCompose](#caching-task-packagecompose)

## About

The main responsibility of this plugin is to build the CRX/AEM package.

Provides CRX package related tasks: `packageCompose`, `packageDeploy`, `packageActivate`, `packagePurge` etc.

Should be applied to all projects that are composing CRX packages.

To apply plugin use snippet:

```kotlin
plugins {
    id("com.cognifide.aem.package")
}
```

This plugin implicitly applies also [Common Plugin](common-plugin.md).

## Task `packageCompose`

Compose CRX package from JCR content and bundles.

Inherits from Gradle's [ZIP](https://docs.gradle.org/3.5/dsl/org.gradle.api.tasks.bundling.Zip.html) task.

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
            vault {
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
               bundlePath.set(""**/install/*.jar"")
               bundleChecking.set(BundleChecking.FAIL)
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
However, only if it is required (adapting the project to fit convention is more recommended approach), the following snippet might be useful, to know how to customize particular values:

```kotlin
tasks {
    packageCompose {
        // Controlling ZIP file name by properties coming from inherited Gradle ZIP task: https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Zip.html
        archiveBaseName.set("my-package")

        // Controlling values visible in CRX Package Manager
        vault { 
            properties.set(mapOf(
                "acHandling" to "merge_preserve",
                "requiresRoot" to false
            ))
            name.set(archiveBaseName)
            group.set(project.group)
        }
    }
}
```

### CRX package validation

The built package is validated using the [OakPAL tool](https://github.com/adamcin/oakpal).  
It helps to prevent the situation when CRX Package Manager reports messages like _Package installed with errors_.  
Also gives immediate feedback for mistakes taken when using the copy-pasting technique,  
forgetting about using entities ('&' instead of '&'), missing XML namespaces, and much more.

By default, **nothing needs to be configured** so that the [default plan](https://github.com/Cognifide/gradle-aem-plugin/blob/master/src/main/resources/com/cognifide/gradle/aem/package/OAKPAL_OPEAR/plan.json) will be in use.  
However, more detailed checks could be provided by configuring the base artifact called Opear file containing OakPAL checks.

It could be done via the following snippet ([ACS AEM Commons OakPAL Checks](https://github.com/Adobe-Consulting-Services/acs-aem-commons/tree/master/oakpal-checks) as an example):

```kotlin
aem {
    `package` {
        validator {
            base("com.adobe.acs:acs-aem-commons-oakpal-checks:4.3.4")
        }
    }
}
```

To use custom checks, the only left thing is to reference them in the custom plan.  
Simply create file _plan.json_ under path [src/aem/package/validator/OAKPAL\_OPEAR](../src/asset/package/validator/OAKPAL_OPEAR/plan.json)

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

See [plan documentation](http://adamcin.net/oakpal/the-oakpal-plan.html) and [examples](https://github.com/adamcin/oakpal/blob/master/core/src/test/resources/OakpalPlanTest/fullPlan.json) provided by the OakPAL tool.

There could be many plan files created. To validate the CRX package using a different plan than the default one, specify the property `package.validator.planName=<fileName>`.

Notice that running OakPAL requires to have included in CRX package up-to-date node type definitions coming from AEM instance.  
Such definitions could be manually downloaded using the CRXDE Lite interface (_Tools / Export Node Type_) and put inside the CRX package.  
After installing some dependent CRX packages, the list of exported node types may change.

To keep it up-to-date, the plugin is synchronizing node types from one of the available AEM instances automatically.  
Synchronized file containing node types, later used when building CRX packages is placed at path [_src/aem/package/validator/initial/META-INF/vault/nodetypes.sync.cnd_](../src/asset/package/validator/initial/META-INF/vault/nodetypes.cnd) (location could be tuned by property `package.validator.cndSync.file`. Remember to save this file in VCS, so that CRX package validation will not fail on e.g CI server where AEM instance could be not available.

To configure node types synchronization behavior, use property `package.validator.cndSync.type=<option>`. Available options:

*   _always_ - assuming that AEM instance is always available so that the node types file is synchronized right before validation,
*   _preserve_ (default) - node types file is synchronized only when it does not exist,
*   _never_ - node types file is never synchronized.

The package validation report is saved at path relative to project building CRX package: _build/packageValidator/report.json_.

### Including additional OSGi bundle into the CRX package

Use a dedicated task method named `installBundle`, for example:

```kotlin
tasks {
    packageCompose {
        installBundle("com.github.mickleroy:aem-sass-compiler:1.0.1")
    }
}
```

Note that the method accepts both dependency notations and URLs - bundles could be downloaded from Maven repositories or HTTP/SFTP/SMB servers.

If the bundle is built by another project:

```kotlin
tasks {
    packageCompose {
        installBundleProject(":core") // must apply plugin 'com.cognifide.aem.bundle'
    }
}
```

When using methods `installBundle*` there is an ability to pass a lambda to customize options like bundle run mode or specify a custom install path for a particular package only.

For reference, see usage above in [AEM Multi-Project Example](https://github.com/Cognifide/gradle-aem-multi/blob/master/aem/common/build.gradle.kts).

### Nesting CRX packages

Use a dedicated task method named `nestPackage`, For example:

```kotlin
tasks {
    packageCompose {
        nestPackage("com.adobe.cq:core.wcm.components.all:2.4.0")
        nestPackage("com.adobe.cq:core.wcm.components.examples:2.4.0")
    }
}
```

Note that the method accepts both dependency notations and URLs - packages could be downloaded from Maven repositories or HTTP/SFTP/SMB servers.

If the package is built by another project:

```kotlin
tasks {
    packageCompose {
        nestPackageProject(":ui.content") // must apply plugin 'com.cognifide.aem.package'
    }
}
```

### Assembling packages (merging all-in-one)

Let's assume the following project structure:

*   _build.gradle.kts_ (project `:`, no source files at all)
*   _core/build.gradle.kts_ (project `:core`, OSGi bundle only)
*   _ui.apps/build.gradle.kts_ (project `:ui.apps`, JCR content only)
*   _ui.content/build.gradle.kts_ (project `:ui.content`, JCR content only)

File content of _build.gradle.kts_:

```kotlin
plugins {
    id("com.cognifide.aem.package")
}

aem {
    tasks {
        packageCompose {
            installBundleProject(":core")
            mergePackageProject(":ui.apps")
            mergePackageProject(":ui.content")
        }
    }    
}
```

When building via command:  `gradlew :build`, then the effect will be a CRX package with merged JCR content from projects `:ui.apps`, `:ui.content` and OSGi bundle built by the project `:core`. [Vault workspace filters](http://jackrabbit.apache.org/filevault/filter.html) (defined in file _filter.xml_) from `:ui.apps` and `:ui.content` sub-projects will be combined into a single one automatically. However, one rule must be kept while developing a multi-module project: **all Vault filter roots of all projects must be exclusive**.

Gradle AEM Plugin is configured in a way that a single Gradle project could have:

*   JCR content,
*   source code to compile OSGi bundle,
*   both.

It is worth knowing a difference when comparing to a package built by Maven and [Content Package Maven Plugin](https://helpx.adobe.com/pl/experience-manager/6-4/sites/developing/using/vlt-mavenplugin.html).  
When using it, there is a need for many more modules. A separate one for building the OSGi bundle and another one for building the CRX package and nesting built OSGi bundle.  
When using GAP, there is just much more **flexibility** in organizing project structure. By mixing usages of methods `nestPackage*`, `installBundle*` or `mergePackage*` there is the ability to create any assembly CRX package with the content of any type without restructuring the project.

By default, the GAP will ensure:

*   Running unit tests for all sub-projects providing OSGi bundles to be put under the install path inside the built assembly package when using the method `installBundleProject`.   
    To disable that behavior, set property `package.bundleTest=false`.
*   Running package validations for all sub-projects providing CRX package to be nested when using the method `nestPackageProject`.   
    To disable that behavior, set property `package.nestedValidation=false`.

### Expandable properties

In exactly the same way as it works for instance files, properties can be expanded inside metadata files of the package being composed.

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

*   `aem` - [AemExtension](../src/main/kotlin/com/cognifide/gradle/aem/AemExtension.kt) object,
*   `definition` - [VaultDefinition](../src/main/kotlin/com/cognifide/gradle/aem/common/pkg/vault/VaultDefinition.kt) object,
*   `rootProject` - project with a directory in which _settings.gradle_ is located,
*   `project` - current project.

This feature is especially useful to generate valid _META-INF/properties.xml_ file, the below [template](../src/asset/package/defaults/META-INF/vault/properties.xml) is used by plugin by default:

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

Also, file _nodetypes.cnd_ is dynamically expanded from the [template](../src/main/resources/com/cognifide/gradle/aem/package/META-INF/vault/nodetypes.cnd) to generate a file containing all node types from all sub-packages being merged into an assembly package.

Each JAR file in separate _hooks_ directory will be combined into a single directory when creating an assembly package.

### Publishing packages

Simply add the following snippet to file _build.gradle.kts_ for each project applying package plugin.

```kotlin
plugins {
    id("com.cognifide.aem.package")
    id("maven-publish")
}

publishing {
    repositories {
        maven {
            // specify here e.g Nexus URL and credentials
        }   
    }

    publications {
        create<MavenPublication>("maven") {
            artifact(common.publicationArtifact("packageCompose"))
        }
    }
}
```

To publish a package to a repository (upload it to e.g Nexus repository) simply run one of [publish tasks](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:tasks) (typically `publish`).

It might be worth to configure [publishing repositories](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:repositories) globally. Consider moving `publishing { repositories { /* ... */ } }` section to root project's _build.gradle.kts_ into the section `allprojects { /* ... */ }`. Then defining publishing repositories in each subproject will be no longer necessary.

## Task `packagePrepare`

Processes CRX package metadata - combines default files provided by the plugin itself with overridden ones.  
Covers extracted logic being initially a part of the task `packageCompose`. The reason for separation is effectively better Gradle caching.

## Task `packageValidate`

Validates a composed CRX package.

For setting common options see section [CRX package validation](#crx-package-validation).

For setting project/package-specific options use the snippet below:

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

Use any of the below properties to customize CRX package deployment behavior:

*   `package.deploy.awaited=false` - disable stability & health checks after deploying the CRX package.
*   `package.deploy.distributed=true` - use an alternative form of deployment. At first, deploys CRX package to author instances, then triggers replication of the CRX package so that it will be installed also on publish instances.

*   `instance.packageManager.deployAvoidance=false` - disable skipping of deploying package when a locally built package does not differ to package currently deployed on AEM instance,
*   `instance.packageManager.uploadForce=false` - disable force installation (by default even unchanged CRX package is forced to be reinstalled)
*   `instance.packageManager.installRecursive=false` - disable automatic installation of sub-packages located inside the CRX package being deployed.
*   `instance.packageManager.uploadRetry=n` - customize the number of retries being performed after failed CRX package upload.
*   `instance.packageManager.installRetry=n` - customize the number of retries being performed after failed CRX package install.
*   `instance.packageManager.workflowToggle=[id1=true,id2=false,...]` - temporarily enable or disable AEM workflows during deployment e.g when the CRX package contains generated DAM asset renditions so that regeneration could be avoided and deploy time reduced. For example: `-Ppackage.deploy.workflowToggle=[dam_asset=false]`. Workflow ID _dam\_asset_ is a shorthand alias for all workflows related to DAM asset processing.

## Task `packageUpload`

Upload composed CRX package into AEM instance(s).

## Task `packageDelete`

Delete uploaded CRX package from AEM instance(s).

## Task `packageInstall`

Install uploaded CRX package on AEM instance(s).

## Task `packageUninstall`

Uninstall uploaded the CRX package on AEM instance(s).

To prevent data loss, this unsafe task execution must be confirmed by the command line flag `-Pforce`.

## Task `packagePurge`

Fail-safe combination of `packageUninstall` and `packageDelete`.

To prevent data loss, this unsafe task execution must be confirmed by the command line flag `-Pforce`.

## Task `packageActivate`

Replicate installed CRX package to other AEM instance(s).

## Known issues

### Caching task `packageCompose`

Expandable properties with dynamically calculated value (unique per build) like `created` and `buildCount` are not used by default generated properties file intentionally,  
because such usages will effectively forbid caching `packageCompose` task and it will be never `UP-TO-DATE`.
