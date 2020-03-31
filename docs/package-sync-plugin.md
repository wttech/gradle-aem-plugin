[![Cognifide logo](cognifide-logo.png)](http://cognifide.com)

<p>
  <img src="logo.png" alt="Gradle AEM Plugin"/>
</p>

# Package sync plugin

  * [About](#about)
  * [Task packageSync](#task-packagesync)
     * [Cleaning features](#cleaning-features)
     * [Default cleaning configuration](#default-cleaning-configuration)
     * [Rendition cleaning configuration](#rendition-cleaning-configuration)
     * [Using alternative transfer type](#using-alternative-transfer-type)
     * [Downloading package options](#downloading-package-options)
     * [Copying or cleaning content only](#copying-or-cleaning-content-only)
     * [Filter file at custom path](#filter-file-at-custom-path)
     * [Filter roots specified explicitly](#filter-roots-specified-explicitly)
  * [Task packageConfig](#task-packageconfig)
     * [Synchronizing configuration for built code](#synchronizing-configuration-for-built-code)
     * [Synchronizing configuration for specified PID](#synchronizing-configuration-for-specified-pid)
  * [Task packageVlt](#task-packagevlt)
  * [Known issues](#known-issues)
     * [Vault tasks parallelism](#vault-tasks-parallelism)

## About

Provides tasks for JCR content synchronization using running AEM instance.
Allows to download JCR content in automated manner so manual editing files like *.content.xml* might be redundant.

To apply plugin use snippet:

```kotlin
plugins {
    id("com.cognifide.aem.package.sync")
}
```

## Task `packageSync`

Check out then clean JCR content. 

JCR content to be synchronized from AEM instance repository is determined by [workspace filter file](https://jackrabbit.apache.org/filevault/filter.html) located at path *src/main/content/META-INF/vault/sync.xml*.
However, when this file does not exist, plugin as a fallback will use same file used when composing CRX package which is *src/main/content/META-INF/vault/filter.xml*.

To exclude some files from synchronization, simply copy *filter.xml* file and create file named *sync.xml*. Then add exclusion in a following way:

```xml
<filter root="/content/dam/example">
    <exclude pattern=".*\.gif"/>
</filter>
```

### Cleaning features

Cleaning assumes advanced JCR content normalization to minimize changes visible in VCS after each synchronization.

* unwanted JCR properties removal (with path based inclusion / exclusion rules),
* unwanted JCR mixin types removal,
* unwanted files removal,
* unused XML namespaces removal,
* flattening files (renaming e.g *_cq_dialog/.content.xml* to *_cq_dialog.xml*),
* preserving state of parent files for each Vault filter root (by backup mechanism),
* hooks for custom cleaning rules / processing *.content.xml* files.

### Default cleaning configuration

```kotlin
aem {
    tasks {
        packageSync {
            cleaner {
                filesDotContent {
                    include("**/.content.xml")
                }
                filesDeleted {
                    include(
                        "**/.vlt",
                        "**/.vlt*.tmp",
                        "**/install/*.jar"
                    ) 
                }
                filesFlattened { 
                    include(
                        "**/_cq_design_dialog/.content.xml",
                        "**/_cq_dialog/.content.xml",
                        "**/_cq_htmlTag/.content.xml",
                        "**/_cq_template/.content.xml"
                    )
                }
                propertiesSkipped.set(listOf(
                    pathRule("jcr:uuid", listOf("**/home/users/*", "**/home/groups/*")),
                    pathRule("cq:lastModified*", listOf("**/content/experience-fragments/*")),
                    "jcr:lastModified*",
                    "jcr:created*",
                    "jcr:isCheckedOut",
                    "cq:lastReplicat*",
                    "dam:extracted",
                    "dam:assetState",
                    "dc:modified",
                    "*_x0040_*"
                ))
                mixinTypesSkipped.set(listOf(
                    "cq:ReplicationStatus",
                    "mix:versionable"
                ))
                namespacesSkipped.set(true)
                parentsBackupEnabled.set(true)
                parentsBackupSuffix.set(".bak")
                lineProcess { file, line -> normalizeLine(file, line) }
                contentProcess { file, lines -> normalizeContent(file, lines) }
            }
        }
    }
}
```

### Rendition cleaning configuration

Cleaning could also ensure that AEM renditions will be never saved in VCS. Also any additional properties could be cleaned.
For such cases, see configuration below:

```kotlin
aem {
    tasks {
        packageSync {
            cleaner {
                propertiesSkipped.addAll(listOf(
                    pathRule("dam:sha1", listOf(), listOf("**/content/dam/*.svg/*")),
                    pathRule("dam:size", listOf(), listOf("**/content/dam/*.svg/*")),
                    "cq:name",
                    "cq:parentPath",
                    "dam:copiedAt",
                    "dam:parentAssetID",
                    "dam:relativePath"
                ))
                mixinTypesSkipped.addAll(listOf(
                    pathRule("dam:Thumbnails", listOf(), listOf("**/content/dam/*"))
                ))
                filesDeleted {
                    include(
                        "**/.vlt",
                        "**/.vlt*.tmp",
                        "**/install/*.jar",
                        "**/_jcr_content/folderThumbnail.dir/*",
                        "**/_jcr_content/folderThumbnail/*",
                        "**/_jcr_content/folderThumbnail",
                        "**/_jcr_content/renditions/**"
                    )
                    exclude(
                        "**/_jcr_content/renditions/original.dir/.content.xml",
                        "**/_jcr_content/renditions/original"
                    )
                }
            }  
        }
    }
}
```

### Using alternative transfer type

Available transfer types: *package_download* (default) and *vlt_checkout*.

```bash
gradlew :site.demo:packageSync -Ppackage.sync.transfer=vlt_checkout
```

### Downloading package options

When transfer type is set to *package_download* then it is also possible to...

Download package only without extracting:

```bash
gradlew :site.demo:packageSync -Ppackage.sync.downloader.extract=false
```

Download, delete all previous JCR root contents then extract fresh content:

```bash
gradlew :site.demo:packageSync -Pforce
```

### Copying or cleaning content only

Available mode types: *copy_and_clean* (default), *clean_only* and *copy_only*.

```bash
gradlew :site.demo:packageSync -Ppackage.sync.mode=clean_only
```

### Filter file at custom path
   
```bash
gradlew :site.demo:packageSync -Pfilter.path=custom-filter.xml
gradlew :site.demo:packageSync -Pfilter.path=src/main/content/META-INF/vault/custom-filter.xml
gradlew :site.demo:packageSync -Pfilter.path=C:/aem/custom-filter.xml
```

### Filter roots specified explicitly
   
```bash
gradlew :site.demo:packageSync -Pfilter.roots=[/etc/tags/example,/content/dam/example]
```

## Task `packageConfig`

Synchronizes OSGi configuration as XML files put into JCR content.

First of all, changing OSGi configuration by hand using GUI dialog when accessing */system/console/configMgr* is not recommended.
Instead, configuration changes should be applied by providing OSGi config node in built CRX package under path _/apps/${appName}/config_.
Such file could be created from scratch, but this is generally speaking error-prone process, because of escaping forbidden characters and delimiters, type casts etc.

To simplify that process, task `packageConfig` could be used. 
It will download actual OSGi configuration values as XML file and put it at desired path to be later deployed within built CRX package.

Alternatively, customized values could be set on OSGi configuration GUI dialog then task `packageConfig` could be run to synchronize changes and save them in VCS in a proper format.
However that approach has a little disadvantage. After changing anything by hand on OSGi configuration dialog, changes done in XML files for appropriate OSGi configuration might be longer reflected on dialog after installing CRX package.
Then, as a workaround, try with deleting configuration using button on dialog then update values in XML and deploy package again to restore binding.

### Synchronizing configuration for built code

```bash
gradlew :app:aem:ui.apps:packageConfig
```

Output:

```
Synchronized OSGi configuration XML file(s) (1) matching PID 'com.company.example.aem.core.*':
/Users/krystian.panek/Projects/gradle-aem-multi/app/aem/ui.apps/src/main/content/jcr_root/apps/example/config/com.company.example.aem.core.schedulers.SimpleScheduledTask.xml
```

### Synchronizing configuration for specified PID

```bash
gradlew :app:aem:ui.apps:packageConfig -Ppackage.config.pid=org.apache.sling.jcr.*
```

Output:

```
Synchronized OSGi configuration XML file(s) (20) matching PID 'org.apache.sling.jcr.*':
/Users/krystian.panek/Projects/gradle-aem-multi/app/aem/ui.apps/src/main/content/jcr_root/apps/example/config/org.apache.sling.jcr.base.internal.LoginAdminWhitelist.xml
/Users/krystian.panek/Projects/gradle-aem-multi/app/aem/ui.apps/src/main/content/jcr_root/apps/example/config/org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment-cq-assets.xml
...
/Users/krystian.panek/Projects/gradle-aem-multi/app/aem/ui.apps/src/main/content/jcr_root/apps/example/config/org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet.xml
```

## Task `packageVlt`

Execute any JCR File Vault command. 

For instance, to reflect `instanceRcp` functionality, command below could be executed:

```bash
gradlew :site.demo:packageVlt -Ppackage.vlt.command='rcp -b 100 -r -u -n http://admin:admin@localhost:4502/crx/-/jcr:root/content/dam/example http://admin:admin@localhost:4503/crx/-/jcr:root/content/dam/example' 
```

For more details about available parameters, please visit [VLT Tool documentation](https://helpx.adobe.com/experience-manager/6-4/sites/developing/using/ht-vlttool.html).

While using task `sync` be aware that Gradle requires to have working directory with file *build.gradle.kts* in it, but Vault tool can work at any directory under *jcr_root*. To change working directory for Vault, use property `aem.vlt.path` which is relative path to be appended to *jcr_root* for project task being currently executed.

## Known issues

### Vault tasks parallelism

Vault tool current working directory cannot be easily configured, because of its API. AEM plugin is temporarily changing current working directory for Vault, then returning it back to original value.
In case of that workaround, Vault tasks should not be run in parallel (by separated daemon processed / JVM synchronization bypassed), because of potential unpredictable behavior.
