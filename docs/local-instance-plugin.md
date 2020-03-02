
[![Cognifide logo](cognifide-logo.png)](http://cognifide.com)

<p>
  <img src="logo.png" alt="Gradle AEM Plugin"/>
</p>

# Local Instance plugin

* [About](#about)
* [Instance file structure](#instance-file-structure)
* [Task instanceSetup](#task-instancesetup)
* [Task instanceResetup](#task-instanceresetup)
* [Task instanceCreate](#task-instancecreate)
    * [Configuration of AEM instance source (JAR file or backup file)](#configuration-of-aem-instance-source-jar-file-or-backup-file)
    * [Pre-installed OSGi bundles and CRX packages](#pre-installed-osgi-bundles-and-crx-packages)
    * [Customization of extracted instance files (optional)](#customization-of-extracted-instance-files-optional)
    * [Customization of sling.properties files](#customization-of-slingproperties-files)
* [Task instanceBackup](#task-instancebackup)
    * [Work with remote instance backups](#work-with-remote-instance-backups)
* [Task instanceDestroy](#task-instancedestroy)
* [Task instanceUp](#task-instanceup)
* [Task instanceDown](#task-instancedown)
* [Task instanceRestart](#task-instancerestart)
* [Task instanceResolve](#task-instanceresolve)

## About

Provides instance related tasks: `instanceUp`, `instanceDown`, `instanceSetup`, `instanceBackup`, `instanceSetup`, `instanceCreate` etc.
Allows to create & customize AEM instances on local file system and control them. Also provides support for automated backups and restoring.

Should be applied only at root project / only once within whole build.

To apply plugin use snippet:

```kotlin
plugins {
    id("com.cognifide.aem.instance.local")
}
```

This plugin implicitly applies [Instance Plugin](instance-plugin.md).

## Instance file structure

By default, instance file are stored directly in project, under so called main AEM module usually named *aem*.
Ensure having directory *aem/.instance* ignored in VCS and excluded from indexing by IDE.

![Instance file structure](docs/instance-file-structure.png)

## Task `instanceSetup`

Performs initial setup of local AEM instance(s). Automated version of `instanceCreate instanceUp instanceSatisfy packageDeploy`.

## Task `instanceResetup`

Combination of `instanceDown instanceDestroy instanceSetup`. Allows to quickly back to initial state of local AEM instance(s).

To prevent data loss, this unsafe task execution must be confirmed by parameter `-Pforce`.

## Task `instanceCreate`
 
Create AEM instance(s) at local file system. Extracts *crx-quickstart* from downloaded JAR and applies configuration according to [instance definitions](#defining-instances-via-properties-file). 

### Configuration of AEM instance source (JAR file or backup file)

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

* `auto` - Create instances from most recent backup (remote or local) or fallback to creating from the scratch if there is no backup available. Default mode.
* `scratch` - Force creating instances from the scratch.
* `backup_any` - Force using any backup available at local or remote source.
* `backup_remote` - Force using backup available at remote source (specified as `localInstance.backup.downloadUrl` or `localInstance.backup.uploadUrl`).      
* `backup_local` - Force using local backup (created by task `instanceBackup`).

When mode is different than `scratch`, then backup ZIP file selection rule could be adjusted:

```kotlin

aem {
    localInstance {
        backup {
            selector = {  // default implementation below
                val name = prop.string("localInstance.backup.name") ?: ""
                when {
                    name.isNotBlank() -> firstOrNull { it.fileEntry.name == name }
                    else -> firstOrNull()
                }
            }
        }
    }
}

```

Notice that, default selector assumes that most recent backup will be selected.
Ordering by file name including timestamp then local backups precedence when backup is available on both local & remote source.
Still, backup selector could select exact backup by name when property `localInstance.backup.name` is specified.

### Pre-installed OSGi bundles and CRX packages

Use dedicated section:

```kotlin
aem {
    localInstance {
        install {
            files {
                download("http://my-company.com/aem/packages/my-package.zip")
            }
        }
    }
}
```

Files section works in a same way as in [instance satisfy task](#task-instancesatisfy).
For more details see AEM [File Install Provider](https://helpx.adobe.com/experience-manager/6-5/sites/deploying/using/custom-standalone-install.html#AddingaFileInstallProvider) documentation.

### Customization of extracted instance files (optional)

The plugin allows us to override or provide extra files to local AEM instance installations.
This behavior could be customized by `localInstance` section of plugin DSL:

```kotlin
aem {
    localInstance {
        rootDir = file(".instance") // path under which local instance files are stored
        overridesDir = file("gradle/instance/local") // path with directories instance specific (common, author, publish) holding files that can override default instance files
        expandProperties = mapOf() // place where additional properties can be defined
        expandFiles = listOf( // file patterns which allows using variables inside
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

Besides `expandProperties`, there is one predefined variable [instance](src/main/kotlin/com/cognifide/gradle/aem/common/instance/LocalInstance.kt) 
that can be used to customize scripts and other files 
(see [start script](src/main/resources/com/cognifide/gradle/aem/instance/local/start)).

### Customization of `sling.properties` files

An example could be providing additional configuration for the `sling.properties` file when running AEM 6.5 on Java 11.
It is needed to override only two properties in the final `sling.properties` file: 
* `org.osgi.framework.system.packages.extra`
  * which describe extra packages that will be exported in the system bundle
  * in Java 11 system bundle by default exports fewer API classes than in Java 8
  * sometimes we need to extend this set to make pre-Java 11 bundles work
* and `org.osgi.framework.bootdelegation`
  * to solve known [FELIX-6184](https://issues.apache.org/jira/browse/FELIX-6184) issue 

To achieve that, add file with your props under `gradle` directory in your project:
`gradle/instance/local/common/crx-quickstart/conf/sling.properties` 

```properties
org.osgi.framework.system.packages.extra=org.apache.sling.launchpad.api;version\=1.2.0 ${org.apache.sling.launcher.system.packages},com.sun.org.apache.xpath.internal;version\="{dollar}{felix.detect.java.version}",com.sun.activation.registries;version\="{dollar}{felix.detect.java.version}"
org.osgi.framework.bootdelegation=sun.*,com.sun.*,jdk.internal.reflect,jdk.internal.reflect.*
```

Now, when creating a new instance with this configuration, only those two properties will get overridden in `slin.properties`.

Notice, that adding the file under `gradle/instance/local/common` will apply it both for author and publish instances.

## Task `instanceBackup`

Archives local AEM instance(s) into ZIP file. Provides automated way to perform [offline backup](https://helpx.adobe.com/pl/experience-manager/6-5/sites/administering/using/backup-and-restore.html#OfflineBackup). Requires having instance(s) down.

To perform [online backup](https://helpx.adobe.com/pl/experience-manager/6-5/sites/administering/using/backup-and-restore.html#OnlineBackup) consider to [implement custom AEM task](#implement-custom-aem-tasks) which will reflect cURL functionality from section [Automating AEM Online Backup](https://helpx.adobe.com/pl/experience-manager/6-5/sites/administering/using/backup-and-restore.html#OnlineBackup).

The most recent file created by this task will be reused automatically while running task `instanceCreate`.

Backup files are stored at path relative to project that is applying plugin `com.cognifide.aem.instance`.
Most often it will be path: *build/aem/instanceBackup/local/xxx.backup.zip*. It could be overridden by writing:

```kotlin
aem {
    localInstance {
        backup {
            localDir = file("any/other/directory")
        }
    }
}
```

Instance backups created by this task could be used later by [create task](#task-instancecreate).

It is also possible to upload only previously created local backup. In that case specify mode property by running `gradlew instanceBackup -Pinstance.backup.mode=upload_only`.
Available modes: *zip_and_upload* (default), *zip_only*, *upload_only*.

### Work with remote instance backups

Backups can be automatically downloaded and uploaded from remote server.
 
Minimal requirement to have it working is only to specify `localInstance.backup.uploadUrl`.
By having only this upload property specified, plugin will automatically download most recent backup found in directory determined by upload URL.

It is also possible to specify second property `localInstance.backup.downloadUrl` which will cause that concrete backup will be always in use.
By having only this download property specified, plugin will not automatically upload any backups.

Backup files created, by default, have suffix .backup.zip. This matters in case of resolving backups from remote sources to distinguish AEM backups from other files. Most often it is not needed to update it.
These few lines in *gradle.properties* files are required to have automatic two-way backups working:

```ini
localInstance.backup.uploadUrl=sftp://example.com/aem/packages
fileTransfer.sftp.user=foo
fileTransfer.sftp.password=pass
```

To use a custom suffix instead of the default one, `localInstance.backup.suffix` property has to be set in *gradle.properties* file:
```ini
localInstance.backup.suffix=.backup.custom.zip
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
                fileTransfer.download("s3://packages/my-package.zip")
                fileTransfer.upload("s3://packages", `package`)
                // etc
            }
        }
    }
}
```

## Task `instanceDestroy` 

Destroy local AEM instance(s).

To prevent data loss, this unsafe task execution must be confirmed by parameter `-Pforce`.
    
## Task `instanceUp`

Turn on local AEM instance(s).

## Task `instanceDown`

Turn off local AEM instance(s).

## Task `instanceRestart`

Turn off and then turn on local AEM instance(s).

## Task `instanceResolve`

Resolve instance files from remote sources before running other tasks.

Files considered:

* local instance source files (backup ZIP and AEM quickstart JAR & license file used by [create task](#task-instancecreate))

This task might be also useful to check amended configuration to verify HTTP urls, SMB / SSH credentials etc and fail fast when they are wrong.
