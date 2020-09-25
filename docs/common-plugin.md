![Cognifide logo](cognifide-logo.png)

![Gradle AEM Plugin](logo.png)

# Common plugin

*   [About](#about)
*   [Instance conventions](#instance-conventions)
*   [Defining instances by a properties file](#defining-instances-by-a-properties-file)
*   [Defining instances by a build script](#defining-instances-by-a-build-script)
    *   [Instance filtering](#instance-filtering)
    *   [Instance URL credentials encoding](#instance-url-credentials-encoding)
    *   [Implementing tasks](#implementing-tasks)
        *   [Instance services](#instance-services)
        *   [Defining the CRX package via code then downloading and sharing it using an external HTTP endpoint](#defining-crx-package-via-code-then-downloading-and-sharing-it-using-external-http-endpoint)
        *   [Calling AEM endpoints / making any HTTP requests](#calling-aem-endpoints--making-any-http-requests)
        *   [Downloading the CRX package from external HTTP endpoint and deploying it on desired AEM instances](#downloading-crx-package-from-external-http-endpoint-and-deploying-it-on-desired-aem-instances)
        *   [Transferring live content CRX package to local AEM instances](#transferring-live-content-crx-package-to-local-aem-instances)
        *   [Working with the content repository (JCR)](#working-with-content-repository-jcr)
        *   [Executing code on AEM runtime](#executing-code-on-aem-runtime)
        *   [Controlling OSGi bundles, components, and configurations](#controlling-osgi-bundles-components-and-configurations)
        *   [Controlling workflows](#controlling-workflows)
        *   [Running Docker image-based tools](#running-docker-image-based-tools)
*   [Properties expanding in an instance or package files](#properties-expanding-in-instance-or-package-files)

## About

```kotlin
plugins {
    id("com.cognifide.aem.common")
}
```

Applied transparently by other plugins. Provides AEM extension to build script / **Gradle AEM DSL** which consists of instance definitions, common configuration, methods for controlling local instances.

It does not provide any tasks but is a base for implementing them on our own.  
Apply other plugins to provide pre-defined, ready-to-use tasks.

## Instance conventions

*   Instance **name** is a combination of _${environment}-${id}_ e.g _local-author_, _integration-publish_ etc.
*   Instance **id** is an instance purpose identifier and must start with the prefix _author_ or _publish_. Sample valid names: _author_, _author1_, _author2_, _author-master_ and _publish_, _publish1_ _publish2,_ etc.
*   Instance **type** indicates the physical type of instance and could be only: _local_ and _remote_. Local means that instance could be created by the plugin automatically under the local file system.
*   Only instances defined as _local_ are considered in command `instanceSetup`, `instanceCreate`, `instanceUp` etc (that comes from the plugin `com.cognifide.aem.instance.local`).
*   All instances defined as _local_ or _remote_ are considered in commands CRX package deployment-related like `instanceProvision`, `packageDeploy`, `packageUpload`, `packageInstall` etc.

Instances could be defined in two ways, via:

*   file `gradle.properties` - recommended approach, by properties convention.
*   build script - dynamic & more customizable approach.

## Defining instances by a properties file

The configuration could be specified through _gradle.properties_ file using dedicated syntax.

`instance.$ENVIRONMENT-$ID.$PROP_NAME=$PROP_VALUE`

| Part                     | Possible values                                                                                                                                                                                                                                                                                                                     | Description                                                                                                                  |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `$ENVIRONMENT`           | `local`, `int`, `stg` etc                                                                                                                                                                                                                                                                                                           | Environment name.                                                                                                            |
| `$ID`                    | `author`, `publish`, `publish2`, etc                                                                                                                                                                                                                                                                                                | Combination of AEM instance type and semantic suffix useful when more than one of instance of same type is being configured. |
| `$PROP_NAME=$PROP_VALUE` | **Local instances:** `httpUrl=http://admin:admin@localhost:4502`<br>`type=local`(or remote)<br>`password=foo`<br>`runModes=nosamplecontent`<br>`jvmOpts=-server -Xmx2048m -XX:MaxPermSize=512M -Djava.awt.headless=true`, `startOpts=...`<br>`debugPort=24502`.<br><br>**Remote instances:** `httpUrl`, `type`, `user`, `password`. | Run modes, JVM opts and start opts should be comma delimited.                                                                |

Default remote instances defined via properties (below lines are optional):

```
instance.local-author.httpUrl=http://localhost:4502
instance.local-publish.httpUrl=http://localhost:4503
```

Example of defining multiple remote instances (that could be [filtered](#instance-filtering)):

```
instance.int-author.httpUrl=http://author.aem-integration.company.com
instance.int-publish.httpUrl=http://aem-integration.company.com
instance.stg-author.httpUrl=http://author.aem-staging.company.com
instance.stg-publish.httpUrl=http://aem-staging.company.com
```

Example for defining remote instance with credentials separated:

```
instance.test-author.httpUrl=http://author.aem-integration.company.com
instance.test-author.user=foo
instance.test-author.password=bar
```

Example for defining remote instance with credentials details included in URL:

```
instance.test-author.httpUrl=http://foo:bar@author.aem-integration.company.com
```

Example for defining local instances (created on the local file system):

```
instance.local-author.httpUrl=http://localhost:4502
instance.local-author.type=local
instance.local-author.debugPort=14502
# debugAddress can be used to enable access to debug port from outside the local network (Java 9+)
instance.local-author.debugAddress=*
instance.local-author.runModes=nosamplecontent
instance.local-author.jvmOpts=-server -Xmx1024m -XX:MaxPermSize=256M -Djava.awt.headless=true

instance.local-publish.httpUrl=http://localhost:4503
instance.local-publish.type=local
instance.local-publish.debugPort=14503
instance.local-publish.debugAddress=*
instance.local-publish.runModes=nosamplecontent
instance.local-publish.jvmOpts=-server -Xmx1024m -XX:MaxPermSize=256M -Djava.awt.headless=true
```

Notice! Remember to define also AEM [source files](local-instance-plugin.md#configuration-of-aem-instance-source-jar-file-or-backup-file).

## Defining instances by a build script

Example usage below. The commented value is an effective instance name.

```kotlin
aem {
    instance {
        local("http://localhost:4502") // local-author
        local("http://localhost:4502") { // local-author
            password = "admin"
            id = "author"
            debugPort = 14502 
        }

        local("http://localhost:4503") // local-publish
        local("http://localhost:4503") { // local-publish
            password = "admin"
            id = "publish"
            debugPort = 14503
        } 

        remote("http://192.168.10.1:4502") { // integration-author1
            user = "user1" 
            password = "password2"
            env = "integration"
            id = "author1"
        } 
        remote("http://192.168.10.1:8080") { // integration-author2
            user = "user1" 
            password = "password2"
            env = "integration"
            id = "author2"
        } 
        remote("http://192.168.10.2:4503") { // integration-publish1
            user = "user2"
            password = "password2"
            env = "integration"
            id = "publish1"
        } 
        remote("http://192.168.10.2:8080") { // integration-publish2
            user = "user2"
            password = "password2"
            env = "integration"
            id = "publish2"
        } 
    }
}
```

### Instance filtering

When there are defined named AEM instances: `local-author`, `local-publish`, `integration-author` and `integration-publish`,  
then it is possible to:

*   deploy (or satisfy) CRX package(s),
*   tail logs,
*   checkout JCR content,

with taking into account:

*   type of environment (local, integration, staging, etc),
*   type of AEM instance (author / publish),

by filtering instances by names, e.g:

```bash
gradlew packageDeploy -Pinstance.name=integration-*
gradlew packageDeploy -Pinstance.name=*-author
gradlew packageDeploy -Pinstance.name=local-author,integration-author
```

The default value of that instance name filter is `${env}-*`, so that typically `local-*`.  
Environment value comes from the system environment variable `ENV` or property `env`.

To deploy only to the author or publish instances:

```bash
gradlew packageDeploy -Pinstance.author
gradlew packageDeploy -Pinstance.publish
```

To deploy only to instances specified explicitly:

```bash
gradlew packageDeploy -Pinstance.list=[http://admin:admin@localhost:4502,http://admin:admin@localhost:4503]
```

Instance URLs must be delimited by a colon.  
Remember to [encode instance user & password](#instance-url-credentials-encoding) properly.

### Instance URL credentials encoding

Remember to encode instance credentials (user & password) when passing it via properties: `instance.$INSTANCE_NAME.httpUrl` or `instance.list=[$INSTANCE_HTTP_URL1,$INSTANCE_HTTP_URL2,...]`.

For example, let's assume that instance is created by [instance plugin](instance-plugin.md) using the following properties:

```ini
instance.local-author.httpUrl=http://localhost:4502
instance.local-author.type=local
instance.local-author.password=gxJMge@6F5ZV#s9j
```

The password is generated by e.g [strong password generator](https://passwordsgenerator.net/) so it has special URL characters.  
Then to be able to use this created instance but remotely and via `instance.list` property on CI, then it should look as below:

```ini
sh gradlew :aem:assembly:full:packageDeploy -Pinstance.list=[http://admin:gxJMge%406F5ZV%23s9j@192.168.123.123:4502]
```

Notice that when a password is specified as separate property `instance.$INSTANCE_NAME.password`, then it does not need to be encoded.  
Otherwise, password value must be encoded by e.g [online URL encoder](https://meyerweb.com/eric/tools/dencoder/).

### Implementing tasks

Most of the built-in tasks logic is based on object `aem` of type [AemExtension](../src/main/kotlin/com/cognifide/gradle/aem/AemExtension.kt).  
It provides concise AEM related API for accessing AEM configuration, synchronizing with AEM instances via specialized instance services of `aem.sync`  
to make tasks implementation a breeze. The options for automating things around AEM are almost unlimited.

#### Instance services

While implementing custom AEM tasks, mix usages of following instance services:

*   `http` - [InstanceHttpClient](../src/main/kotlin/com/cognifide/gradle/aem/common/instance/InstanceHttpClient.kt) - Provides extremely easy to use HTTP client designed especially to be used with AEM (covers basic authentication, allows to use only relative paths instead of full URLs, etc)
*   `packageManager` - [PackageManager](../src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/pkg/PackageManager.kt) - Allows communicating with the CRX Package Manager.
*   `osgiFramework` - [OsgiFramework](../src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/osgi/OsgiFramework.kt) - Controls OSGi framework using [Apache Felix Web Console endpoints](https://felix.apache.org/documentation/subprojects/apache-felix-web-console.html).
*   `repository` - [Repository](../src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/repository/Repository.kt) - Allows communicating with JCR Content Repository.
*   `workflowManager` - [WorkflowManager](../src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/workflow/WorkflowManager.kt) - Allows to temporarily toggle (enable or disabled) change workflow launcher state e.g to disable DAM assets regeneration while deploying the CRX package.
*   `groovyConsole` - [GroovyConsole](../src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/groovy/GroovyConsole.kt) - Allows to execute Groovy code/scripts on AEM instance having [Groovy Console](https://github.com/icfnext/aem-groovy-console) CRX package installed.
*   `status` - [Status](../src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/status/Status.kt) - Allows to read statuses available at [Apache Felix Web Console](https://felix.apache.org/documentation/subprojects/apache-felix-web-console.html).
*   `crx` - [Crx](../src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/crx/Crx.kt) - Allows reading available node types of JCR repository / AEM instance.

#### Defining CRX package via code then downloading and sharing it using an external HTTP endpoint

The below snippet could be used to automatize the creation of production content backups.

```kotlin
aem {
    tasks {
        register("backupProductionAuthor") {
            doLast {
                val pkg = namedInstance("prod-author").sync {
                    downloadPackage {
                        group = "example"
                        name = "backup"
                        description = "Backup of content, tags and DAM"
                        archiveName = "backup-author.zip"
                        filters(
                                "/content/cq:tags/example",
                                "/content/example",
                                "/content/dam/example"
                        )
                    }
                }

                http {
                    basicUser = "foo"
                    basicPassword = "bar"
                    postMultipart("http://my-aem-backup-service.com/package/upload", mapOf("file" to pkg)) 
                }
            }
        }
    }
}
```

#### Calling AEM endpoints / making any HTTP requests

To make an HTTP request to some AEM endpoint (servlet) simply write:

```kotlin
aem {
    tasks {
        register("runHealthCheck") {
            doLast {
                syncInstances {
                    http {
                        get("/bin/example/healthCheck") { checkStatus(it, 200) }
                    }
                }
            }
        }
    }
}
```

There are unspecified AEM instances as the parameter for the method `syncInstances`, so that instances matching [default filtering](#instance-filtering) will be used.

The fragment `{ checkStatus(it, 200) }` could be even omitted because, by default, sync API checks the status code that it belongs to the range \[200,300).

To parse endpoint response as [JSON](http://static.javadoc.io/com.jayway.jsonpath/json-path/2.4.0/com/jayway/jsonpath/DocumentContext.html) (using [JsonPath](https://github.com/json-path/JsonPath)), simply write:

```kotlin
aem {
    tasks {
        register("runHealthCheck") {
            doLast {
                syncInstances {
                    http {
                        val json = get("/bin/example/healthCheck") { asJson(it) }
                        val status = json.read("status") as String

                        if (status != "OK") {
                            throw GradleException("Health check failed on: $instance because status '$status' detected.")
                        }
                    }
                }
            }
        }
    }
}
```

There are also available convenient methods `asStream`, `asString` to be able to process endpoint responses.

#### Downloading the CRX package from external HTTP endpoint and deploying it on desired AEM instances

Below snippet could be used to automatize recovery from content backups (e.g for production or to replicate production content to test environment).

```kotlin

aem {
    tasks {
        register("deployProductionContent") {
            doLast {
                val instances = listOf(
                        instance("http://user:password@aem-host.com") // URL specified directly, could be parametrized by some gradle command line property
                        // namedInstance("local-publish") // reused AEM instance defined in 'gradle.properties'
                )
                val pkg = httpFile { download("https://company.com/aem/backups/example-1.0.0-201901300932.backup.zip") }

                sync(instances) { 
                    packageManager.deploy(pkg) 
                }
            }
        }
    }
}
```

#### Transferring live content CRX package to local AEM instances

Task `contentSync` only downloads and deploys packages when a new live content package will become available and it is not yet deployed on local AEM instances.  
The example below assumes a dynamic instance definition based on Gradle properties.

```kotlin
tasks {
    register("contentSync") {
        dependsOn(":requireProps")
        doLast {
            val sourceInstance = aem.instance(findProperty("contentSyncInstanceUrl")?.toString().orEmpty()).apply {
                user = findProperty("contentSyncUser")?.toString().orEmpty()
                password = findProperty("contentSyncPassword")?.toString().orEmpty()
                sync { packageManager.requireAvailable() }
            }
            val sourcePkg = sourceInstance.sync {
                packageManager.all.asSequence()
                        .filter { it.group == "content-sync" }
                        .maxBy { it.lastTouched }
                        ?: throw GradleException("Cannot find content sync package on $instance!")
            }
            val upToDate = aem.instances.all { it.sync { packageManager.contains(sourcePkg) } }
            if (upToDate) {
                logger.lifecycle("Most recent 'content-sync' package is available on all instances.")
            } else {
                val targetDir = file("build/contentSync").apply {
                    deleteRecursively()
                    mkdirs()
                }
                val targetFile = sourceInstance.sync { packageManager.downloadTo(sourcePkg, targetDir) }
                aem.sync { packageManager.deploy(targetFile) }

                logger.lifecycle("Most recent 'content-sync' package has been deployed to all instances.")
            }
        }
    }
}
```

#### Working with the content repository (JCR)

To make changes in the AEM content repository, use the [Repository](../src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/repository/Repository.kt) instance service which is a part of instance sync tool.

For example, to migrate pages even without using [Groovy Console](https://github.com/icfnext/aem-groovy-console) deployed on some AEM instance, simply write:

```kotlin
aem {
    tasks {
        register("migratePages") {
            description = "Migrates pages to new component"
            doLast {
                syncInstances {
                    repository {
                        node("/content/example")
                            .traverse()
                            .filter { it.type == "cq:PageContent" && it.properties["sling:resourceType"] == "example/components/basicPage" }
                            .forEach { page ->
                                logger.info("Migrating page: ${page.path}")
                                page.saveProperty("sling:resourceType", "example/components/advancedPage")
                            }
                    }
                }
            }
        }
    }
}
```

To create new or update existing nodes, write:

```kotlin
aem {
    tasks {
        register("setupReplicationAgents") {
            description = "Corrects publish replication agent transport URI"
            doLast {
                syncInstances {
                    repository {
                        node("/etc/some-tool/config", mapOf( // shorthand for 'node(path).save(props)'
                            "key" to "value",
                            "flag" to false,
                        ))
                    }
                }
            }
        }
    }
}
```

Under the hood, the repository service is using only AEM built-in [Sling Post Servlet](https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html) so it should work with all AEM instances that are having that servlet accessible (which reflects default AEM configuration).

#### Executing code on AEM runtime

It is also possible to easily execute any code on AEM runtime using the [Groovy Console](https://github.com/icfnext/aem-groovy-console).  
Assuming that on AEM instances there is already installed Groovy Console e.g via task `instanceProvision`, then it is possible to use the [GroovyConsole](../src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/groovy/GroovyConsole.kt) instance service.

```kotlin
aem {
    provisioner {
        deployPackage("https://github.com/icfnext/aem-groovy-console/releases/download/12.0.0/aem-groovy-console-12.0.0.zip")
    }
    tasks {
        register("generatePosts") {
            doLast {
                syncInstances {
                    groovyConsole.evalCode("""
                        def postsService = getService("com.company.example.aem.sites.services.posts.PostsService")

                        println postsService.randomPosts(5)
                    """)
                    // groovyConsole.evalScript("posts.groovy") // if script above moved to 'aem/gradle/groovyScript/posts.groovy'
                }
            }
        }
    }
}
```

#### Controlling OSGi bundles, components, and configurations

Simply use the instance service: [OsgiFramework](../src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/osgi/OsgiFramework.kt).

To restart some bundle after deploying a CRX package, write:

```kotlin
aem {
    tasks {
        packageDeploy {
            doLast {
                syncInstances {
                    osgiFramework.restartBundle("com.adobe.cq.dam.cq-scene7-imaging")
                }           
            }       
        }
    }
}
```

To disable specific OSGi component by its PID value and only on publish instances, write:

```kotlin
aem {
    tasks {
        register("instanceSecure") {
            doLast {
                sync(publishInstances) {
                    osgiFramework.disableComponent("org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet")
                    // osgiFramework.stopBundle("org.apache.sling.jcr.webdav")
                }
            }
        }
    }
}
```

To configure specific OSGi service by its PID:

```kotlin
aem {
    tasks {
        register("enableCrx") {
            doLast {
                sync(publishInstances) {
                    osgiFramework.configure("org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet", mapOf(
                        "alias" to "/crx/server",
                        "dav.create-absolute-uri" to true,
                        "dav.protectedhandlers" to "org.apache.jackrabbit.server.remoting.davex.AclRemoveHandler"
                    ))
                }
            }
        }
    }
}
```

All [CRUD](https://en.wikipedia.org/wiki/CRUD) methods for manipulating OSGi configurations are available. Also for configuration factories.

#### Controlling workflows

Simply use [Workflow Manager](../src/main/kotlin/com/cognifide/gradle/aem/common/instance/service/workflow/WorkflowManager.kt) instance service.

Workflows can be either enabled or disabled by:

```kotlin
aem {
    tasks {
        register("setupWorkflows") {
            doLast {
                syncInstances {
                    workflowManager.workflow("update_asset_create").disable()
                    workflowManager.workflow("update_asset_mod").disable()

                    workflowManager.workflows("dam_asset").forEach { it.enable() } // reverts above using shorthand alias
                }
            }        
        }   
    }   
}
```

Also, it is possible to enable or disable workflows only for a particular action to be performed:

```kotlin
    workflowManager.toggleTemporarily("dam_asset", false) {
        packageManager.deploy(file("my-package.zip"))
    }
```

It is also possible to mix enabling and disabling workflows:

```kotlin
    workflowManager.toggleTemporarily(mapOf(
        "update_asset_create" to false,
        "update_asset_mod" to false
        "update_asset_custom" to true 
    )) {
        // ...
    }
```

#### Running Docker image based tools

Regardless type of Docker runtime used (Desktop or Toolbox), running any Docker images is super-easy via Gradle AEM DSL methods `aem.dockerRun()` or `aem.dockerDaemon()`.  
Under the hood, GAP ensures that volume paths passed to Docker are correct. Also correctly handles streaming process output,  
allows to set expected exit codes and other useful options.

Consider following task registration:

```kotlin
tasks {
    register("runTool")
        doLast {
            aem.runDocker {
                operation("Runnning Docker based tool'")
                image = "any-vendor/any-image"
                volume(file("resources"), "/resources")
                port(8080, 80)
                command = "<any command>"
            }
        }
    }
}
```

Then **running any Docker image based tool** could be simply achieved via running CLI command:

`sh gradlew runTool`.

What is more, it is possible to run any Docker based tool as a daemon.  
Plugin will ensure that after stopping it, corresponding Docker container will be killed automatically.  
Also, all daemon logs are redirected to separated file.

```kotlin
tasks {
    register("sftpServer") {
        doLast {
            aem.runDocker {
                operation("Runnning SFTP Server")
                image = "atmoz/sftp"
                command = "foo:pass:::upload"
                port(2222, 22)
            }
        }
    register("mockServer") {
        doLast {
            aem.runDocker {
                operation("Runnning Duckrails Mock Server")
                image = "iridakos/duckrails:release-v2.1.5"
                volume("duckrails", "/opt/duckrails/db")
                port(8080, 80)
            }
        }
    }
}
```

## Properties expanding in an instance or package files

The properties syntax comes from [Pebble Template Engine](https://github.com/PebbleTemplates/pebble) which means that all its features (if statements, for loops, filters, etc) can be used inside files being expanded.

Expanding properties could be used separately on any string or file source in any custom task by using the method `common.prop.expand()`.
