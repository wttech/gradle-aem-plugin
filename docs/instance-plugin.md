[![Cognifide logo](cognifide-logo.png)](http://cognifide.com)

<p>
  <img src="logo.png" alt="Gradle AEM Plugin"/>
</p>

# Instance plugin

* [About](#about)
* [Task instanceStatus](#task-instancestatus)
* [Task instanceReload](#task-instancereload)
* [Task instanceResolve](#task-instanceresolve)
* [Task instanceProvision](#task-instanceprovision)
* [Task instanceAwait](#task-instanceawait)
* [Task instanceTail](#task-instancetail)
     * [Tailing incidents](#tailing-incidents)
     * [Tailing to console](#tailing-to-console)
     * [Tailing multiple instances](#tailing-multiple-instances)
     * [Standalone tailer tool](#standalone-tailer-tool)
* [Task instanceRcp](#task-instancercp)
* [Task instanceGroovyEval](#task-instancegroovyeval)
* [Task instanceWorkflow](#task-instanceworkflow)

## About

Provides tasks related to remote instance management:  `instanceProvision`, `instanceTail`, `instanceReload`, `instanceAwait` etc.
Allows to interact with remote AEM instances (without direct access to the instance files).

Should be applied only at root project / only once within whole build.

To apply plugin use snippet:

```kotlin
plugins {
    id("com.cognifide.aem.instance")
}
```

This plugin implicitly applies also [Common Plugin](common-plugin.md) and [Runtime Plugin](https://github.com/Cognifide/gradle-common-plugin).

## Task `instanceStatus`

Print actual information about configured AEM instances.

Provides information about physical type of instance (local / remote), current condition (state check) etc.
When task is run with `--info` also prints out information about instance time zone, operating system and Java version.

![Instance status task](instance-status-task.png)

## Task `instanceReload`

Reload OSGi Framework (Apache Felix) on local and remote AEM instance(s).

## Task `instanceResolve`

Resolve instance files from remote sources before running other tasks.

Files considered:

* AEM instance source files (JAR and license) used by [create task](local-instance-plugin.md#task-instancecreate),
* AEM instance backups (ZIP) created via [backup task](local-instance-plugin.md#task-instancebackup),
* CRX packages to be deployed via [provision task](#task-instanceprovision).

This task might be also useful to check amended configuration to verify HTTP urls, SMB / SSH credentials etc and fail fast when they are wrong.

## Task `instanceProvision`

Performs configuration actions for AEM instances in customizable conditions (specific circumstances).
Feature dedicated for pre-configuring AEM instances as of not all things like turning off OSGi bundles is easy realizable via CRX packages.
For instance, provisioning could help to avoid using [OSGi Bundle Disabler](https://adobe-consulting-services.github.io/acs-aem-commons/features/osgi-disablers/bundle-disabler/index.html) and [OSGi Component Disabler](https://adobe-consulting-services.github.io/acs-aem-commons/features/osgi-disablers/component-disabler/index.html) etc and is a more powerful and general approach.
Could be used for AEM related troubleshooting like periodic cache cleaning, restarting OSGi bundle or components before or after CRX package deployment etc.

Sample configuration:

```kotlin
aem {
    instance {
        provisioner {
            enableCrxDe()
            step("setup-replication-author") {
                condition { once() && instance.author }
                sync {
                    repository {
                        save("/etc/replication/agents.author/publish/jcr:content", mapOf(
                                "enabled" to true,
                                "userId" to instance.user,
                                "transportUri" to "http://localhost:4503/bin/receive?sling:authRequestLogin=1",
                                "transportUser" to instance.user,
                                "transportPassword" to instance.password
                        ))
                    }
                }
            }
            step("disable-unsecure-bundles") {
                condition { once() && instance.env == "prod" }
                sync {
                    osgi.stopBundle("org.apache.sling.jcr.webdav")
                    osgi.stopBundle("com.adobe.granite.crxde-lite")

                    instance.awaitUp() // include above in property: 'instance.awaitUp.bundles.symbolicNamesIgnored'
                }
            }
        }
    }
}
```

By running task `instanceProvision`, provisioner will perform all steps for which conditions are met.
Specifying condition could be even omitted, then by default each step will be performed only `once()` 
which means that configured `action {}` (or action set via shorthand `sync {}`) will be executed only once on each AEM instance.

Conditions could be more complex and use helpful methods basing on: 

* guaranteed execution: `once()` <=> `failSafeOnce()`,
* forced execution: `always()`, `never()`,
* time: `repeatAfterDays(n)`, `repeatAfterHours(n)`, `repeatAfterMinutes(n)`, `repeatAfterMillis(n)`,
* counter: `repeatEvery(times)`, `repeatEvery { counter: Long -> Boolean }`,
* probability: `repeatProbably(probability)`.

There are also options for making provisioning more fail-safe, especially when error will be triggered when performing step action.
Then each step may be additionally configured with:

* `continueOnFail = true` - logging error to console instead of breaking build with exception so that next step might be performed,
* `rerunOnFail = false` - disabling performing step again when previously failed. Considered only when using condition `once()` or `failSafeOnce()` and other conditions based on time,
* `retry { afterSquaredSecond(3) }` - redo step action on exception after delay time with distribution like `afterSquaredSecond(n)`, `afterSecond(n)` or custom `after(n, delayFunction)`.

To perform some step(s) selectively, use step name property (values comma delimited, wildcards supported):

```bash
gradlew instanceProvision -Pinstance.provision.step=enable-crxde,...
```

To perform step(s) regardless conditions, use greedy property (may be combined with previous one):

```bash
gradlew instanceProvision -Pinstance.provision.greedy
```

## Task `instanceAwait`

Check health condition of AEM instance(s) of any type (local & remote).

Customize behavior of each particular health check using following lambdas:

```kotlin
aem {
    tasks {
        instanceAwait {
            awaitUp {
                timeout {
                    stateTime.apply {
                        convention(TimeUnit.MINUTES.toMillis(2))
                        set(prop.long("instance.awaitUp.timeout.stateTime"))
                    }
                    constantTime.apply {
                        convention(TimeUnit.MINUTES.toMillis(10))
                        set(prop.long("instance.awaitUp.timeout.constantTime"))
                    }
                }
                bundles {
                    symbolicNamesIgnored.apply {
                        convention(listOf())
                        set(prop.list("instance.awaitUp.bundles.symbolicNamesIgnored"))
                    }
                }
                components {
                    platformComponents.apply {
                        convention(listOf(
                            "com.day.crx.packaging.*", 
                            "org.apache.sling.installer.*"
                        ))
                        set(prop.list("instance.awaitUp.components.platform"))
                    }
                    specificComponents.apply {
                        convention(obj.provider { javaPackages.map { "$it.*" } })
                        set(prop.list("instance.awaitUp.components.specific"))
                    }
                }
                events {
                    unstableTopics.apply {
                        convention(listOf(
                            "org/osgi/framework/ServiceEvent/*",
                            "org/osgi/framework/FrameworkEvent/*",
                            "org/osgi/framework/BundleEvent/*"
                        ))
                    }   set(prop.list("instance.awaitUp.event.unstableTopics"))
                    unstableAgeMillis.apply {
                        convention(TimeUnit.SECONDS.toMillis(5))
                        set(prop.long("instance.awaitUp.event.unstableAgeMillis"))
                    }
                }
            }
        }
    }
}
```

By default, `packageDeploy` task is also awaiting up instances (this could be optionally disabled by property `package.deploy.awaited=false`).
So it is also possible to configure each health check there:

```kotlin
aem {
    tasks {
        packageDeploy {
            awaitUp {
                // ...
            }
        }
    }
}
```

## Task `instanceTail`

Continuosly downloads logs from any local or remote AEM instances.
Detects and interactively notifies about unknown errors as incident reports.

Tailer eliminates a need for connecting to remote environments using SSH protocol to be able to run `tail` command on that servers. 
Instead, tailer is continuously polling log files using HTTP endpoint provided by Sling Framework. 
New log entries are being dynamically appended to log files stored on local file system in a separate file for each environment. 
By having all log files in one place, AEM developer or QA engineer has an opportunity to comportably analyze logs, verify incidents occuring on AEM instances.

To customize tailer behavior, see [Tailer](../src/main/kotlin/com/cognifide/gradle/aem/common/instance/tail/Tailer.kt).

```kotlin
aem {
    instance {
        tailer {
            logFilePath.set(prop.string("instance.tail.logFilePath") ?: "/logs/error.log")
            logListener { instance -> /* ... */ }
            incidentFilter.set(file(prop.string("instance.tail.incidentFilter") ?: "src/aem/instance/tail/incidentFilter.txt"))
            incidentDelay.set(prop.long("instance.tail.incidentDelay") ?: 5000L)
        }
    }
}
```

Log files are stored under directory: *build/instance/tail/${instance.name}/error.log*.

### Tailing incidents

By default, tailer is buffering cannonade of log entries of level *ERROR* in 5 seconds time window then interactively shows notification.
Clicking on that notification will browse to incident log file created containing only desired exceptions. These incident files are stored under directory: *build/aem/instanceTail/${instance.name}/incidents/${timestamp}-error.log*.

Which type of log entries are treated as a part of incident is determined by:

* property `-Pinstance.tail.incidentLevels=[ERROR,WARN]`
* wildcard exclusion rules defined in file which location is controlled by property `-Pinstance.tail.incidentFilter=src/aem/instance/tail/incidentFilter.txt`

Sample content of  *incidentFilter.txt* file, which holds a fragments of log entries that will be treated as known issues (notifications will be no longer shown):

```text
# On Unix OS, it is required to have execution rights on some scripts:
Error while executing script *diskusage.sh
Error while executing script *cpu.sh
```

### Tailing to console

By default, tailer prints all logs to console (with instance name in front and timestamp converted to the machines time zone). To turn it off use:

`./gradlew instanceTail -Pinstance.tail.console=false`

### Tailing multiple instances

Common use case could be to tail many remote AEM instances at once that comes from multiple environments.
To cover such case, it is possible to run tailer using predefined instances and defined dynamically. Number of specified instance URLs is unlimited.

Simply use command:

```bash
gradlew instanceTail -Pinstance.list=[http://admin:admin@192.168.1.1:4502,http://admin:admin@author.example.com]
```

### Standalone tailer tool

Instance tailer could be used as standalone tool beside of e.g Maven based AEM application builds using [Content Package Maven Plugin](https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/vlt-mavenplugin.html).
Just download it from [here](dists/gradle-aem-tailer) (< 100 KB), extract anywhere on disk and run.

## Task `instanceRcp`

Copy JCR content from one instance to another. Sample usages below.

* Using predefined instances with multiple different source and target nodes:

  ```bash
  gradlew :instanceRcp -Pinstance.rcp.source=int-author -Pinstance.rcp.target=local-author -Pinstance.rcp.paths=[/content/example-demo=/content/example,/content/dam/example-demo=/content/dam/example]
  ```

* Using predefined instances with multiple same source and target nodes:

  ```bash
  gradlew :instanceRcp -Prcp.source.instance=stg-author -Pinstance.rcp.target.instance=int-author -Pinstance.rcp.paths=[/content/example,/content/example2]
  ```
  Right side of assignment could skipped if equals to left (same path on both source & target instance).

* Using predefined instances with source and target nodes specified in file:

  ```bash
  gradlew :instanceRcp -Pinstance.rcp.source=int-author -Pinstance.rcp.target=local-author -Pinstance.rcp.pathsFile=paths.txt
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
  gradlew :instanceRcp -Pinstance.rcp.source=http://user:pass@192.168.66.66:4502 -Pinstance.rcp.target=http://user:pass@192.168.33.33:4502 -Pinstance.rcp.paths=[/content/example-demo=/content/example]
  ```

Keep in mind, that copying JCR content between instances, could be a trigger for running AEM workflows like *DAM Update Asset* which could cause heavy load on instance.
Consider disabling AEM workflow launchers before running this task and re-enabling after.

RCP task is internally using [Vault Remote Copy](http://jackrabbit.apache.org/filevault/rcp.html) which requires bundle *Apache Sling Simple WebDAV Access to repositories (org.apache.sling.jcr.webdav)* present in active state on instance.

## Task `instanceGroovyEval`

Evaluate Groovy script(s) on instance(s).

To determine instances on which scripts will be evaluated, simply use [default filtering](common-plugin.md#instance-filtering).
Dedicated place for storing scripts is path [*src/aem/instance/groovyScript*](https://github.com/Cognifide/gradle-aem-multi/tree/master/aem/gradle/groovyScript).

By default, task is running all scripts in alphabetical order located in subdirectory which name is a current project version.

Basic usage:

```bash
gradlew instanceGroovyEval
```

Output:

```
No Groovy scripts matching pattern '1.0.0/**/*.groovy' found in directory: /Users/krystian.panek/Projects/gradle-aem-multi/aem/gradle/groovyScript
```

After adding some scripts 

However, it is easily possible to run scripts matching any path pattern containing wildcards.
 
Simple script usage (suffix *.groovy* is automatically added if missing):

```bash
gradlew instanceGroovyEval -Pinstance.groovyEval.script=content-cleanup
```

Output:

```
Evaluated Groovy script(s)
Succeeded: 2/2=100.00%. Elapsed time: 00:02
```

By default, script outputs, results and exceptions are hidden. To see them, simply turn on info logging level by adding option `-i`.

Output:

```
Groovy script '/Users/krystian.panek/Projects/gradle-aem-multi/src/aem/instance/groovyScript/content-cleanup.groovy' evaluated with success in '00:00:02.520' on LocalInstance(name='local-author', httpUrl='http://localhost:4502')
Groovy script '/Users/krystian.panek/Projects/gradle-aem-multi/src/aem/instance/groovyScript/content-cleanup.groovy' output:
Cleaning content at root '/content/example/demo'
Cleaning page '/content/example/demo/en-gb/jcr:content'
Cleaning page '/content/example/demo/jcr:content'
Cleaning page '/content/example/demo/en-us/jcr:content'
Cleaned content at root '/content/example/demo'
Cleaning content at root '/content/example/live'
Cleaning page '/content/example/live/jcr:content'
Cleaning page '/content/example/live/en-us/jcr:content'
Cleaned content at root '/content/example/live'
Groovy script '/Users/krystian.panek/Projects/gradle-aem-multi/src/aem/instance/groovyScript/content-cleanup.groovy' evaluated with success in '00:00:02.527' on LocalInstance(name='local-publish', httpUrl='http://localhost:4503')
Groovy script '/Users/krystian.panek/Projects/gradle-aem-multi/src/aem/instance/groovyScript/content-cleanup.groovy' output:
Cleaning content at root '/content/example/demo'
Cleaning page '/content/example/demo/en-gb/jcr:content'
Cleaning page '/content/example/demo/jcr:content'
Cleaning page '/content/example/demo/en-us/jcr:content'
Cleaned content at root '/content/example/demo'
Cleaning content at root '/content/example/live'
Cleaning page '/content/example/live/jcr:content'
Cleaning page '/content/example/live/en-us/jcr:content'
Cleaned content at root '/content/example/live'

...

Evaluated Groovy script(s)
Succeeded: 2/2=100.00%. Elapsed time: 00:02
```

To allow seeing particular script errors and continuing evaluation after error, 
verbose mode of [Groovy Console](../src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/groovy/GroovyConsole.kt) instance service need to be disabled.

Below is presented command for multiple scripts usage with output logging and continuation on errors:

```bash
gradlew instanceGroovyEval -i -Pinstance.groovyEval.script=iteration-17/* -Pinstance.groovyConsole.verbose=false 
```

By default, any failed script evaluation will cause that task will also fail.
To avoid that (ignore failures), add parameter `-Pinstance.groovyEval.faulty=false`.


## Task `instanceWorkflow`

Schedules workflow model on resources under the specified path.
Allows to quickly execute workflow models in batch.

Might be useful as scheduling workflows in AEM authoring has constrained number of resources for which workflows might be scheduled at once.
This task has no such limitation and eliminates a need to implement Groovy Script as a workaround.

![Instance workflow task](instance-workflow-task.png)

Sample usages below.

Scheduling workflows for a single resource:

```shell
gradlew instanceWorkflow \
  -Pinstance.workflow.model=dam/asset_processing_on_sdk \
  -Pinstance.workflow.resourcePath=/content/dam/wknd-shared/en/adventures/beervana-portland/adobestock-200192344.jpeg
```

Scheduling workflows for multiple resources:

```shell
gradlew instanceWorkflow \
  -Pinstance.workflow.model=dam/asset_processing_on_sdk \
  -Pinstance.workflow.resourcePath=[/content/dam/wknd-shared/en/adventures/beervana-portland/adobestock-209250305.jpeg,/content/dam/wknd-shared/en/adventures/beervana-portland/adobestock-239751461.jpeg]
```

Scheduling workflows for multiple resources of specified type under root(s):

```shell
% gw instanceWorkflow \
  -Pinstance.workflow.model=dam/asset_processing_on_sdk \
  -Pinstance.workflow.resourcePath=/content/dam/wknd-shared/en/adventures 
  -Pinstance.workflow.resourceType=dam:Asset

```
