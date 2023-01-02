[![Cognifide logo](cognifide-logo.png)](http://cognifide.com)

<p>
  <img src="logo.png" alt="Gradle AEM Plugin"/>
</p>

# Standalone launcher

* [About](#about)
* [Downloads](#downloads)
* [Usages](#usages)
    * [Enhancing Maven Build](#enhancing-maven-build)
    * [Setting up local instance](#setting-up-local-instance)
    * [Deploying packages](#deploying-packages)
    * [Tailing logs](#tailing-logs)
    * [Syncing content](#syncing-content)
    * [Copying content between instances](#copying-content-between-instances)
* [Options](#options)
  * [Saving properties](#saving-properties)
  * [Console output](#console-output)

## About

Some of the GAP features could be useful even when not building AEM application.
Moreover, to run GAP, it is needed to have a project which has at least Gradle Wrapper files and minimal Gradle configuration that applies Gradle AEM Plugin.
To eliminate such ceremony, GAP standalone launcher could be used to be able to use its features with minimal effort, anywhere.
Simply, using e.g bash script - download the GAP launcher run it with regular GAP arguments - all tasks and properties are available to be used.

## Compatibility

| GAP Launcher | AEM Project Archetype |
|--------------|-----------------------|
| 16.0.4       | 39                    | 

See also [plugin compatibility](../README.MD#compatibility).

Note that GAP launcher is trying to scaffold AEM configuration that will work with both versions of AEM (OnPrem & Cloud).
However, the changes made in the AEM archetype are being done independently by Adobe team. This requires that from time to time GAP Launcher need to be aligned to the changes made by Adobe. 

**Contributions in this area are highly welcomed! :)**

Simply saying - don't hesitate to make pull requests to make GAP Launcher compatible again when a new version of Adobe AEM Archetype is available or at least report an issue!

## Downloads

Grab most recent version of launcher from GitHub [releases](https://github.com/Cognifide/gradle-aem-plugin/releases) section.

The launcher on release asset list is a file named **gap.jar**.

## Usages

Below there are some sample usages of standalone launcher.

### Enhancing Maven build

To add Gradle/GAP support to existing Maven build generated from Adobe AEM Archetype...

Choose one of the available options:

A) Gradle files added to the project directly (recommended):

```shell
curl -OJL https://github.com/Cognifide/gradle-aem-plugin/releases/download/16.0.4/gap.jar && java -jar gap.jar && rm gap.jar
```
Consequences:

* newly scaffolded files will be VCS-tracked by the Maven project,
* there will be a single code repository with Maven files supplemented by only a few extra Gradle environment files,
* on fresh setups cloning only a single code repository is needed to set up an automated AEM environment.

B) Gradle files at the root, Maven files nested into the `maven` directory

Consequences:

* newly scaffolded files will be NOT VCS-tracked by the Maven project,
* there will be 2 code repositories:
  * existing one with Maven files (moved to sub dir `maven`),
  * dedicated one for Gradle environment files (root dir/parent of `maven`),
* the advantage is separation of concerns (dedicate repository for environment files and the second one for application code),
* on a fresh setup, cloning both code repositories is required to set up an automated AEM environment.

```shell
curl -OJL https://github.com/Cognifide/gradle-aem-plugin/releases/download/16.0.4/gap.jar && java -jar gap.jar --app-dir=maven && rm gap.jar
```

Demo (to play again refresh the page):

<p align="center">
  <img src="https://github.com/wttech/gradle-aem-plugin/releases/download/assets/gap-launcher.gif" alt="Gradle AEM Plugin - Launcher Demo"/>
</p>

AEM build will gain new capabilities:

- incrementally built Maven modules,
- incrementally deployed in parallel AEM packages to AEM instances,
- easy and fast JCR content synchronization for AEM packages,
- synchronization of OSGi configuration XMLs to AEM packages.

The capabilities mentioned above are available by running dedicated Gradle tasks.
To review available tasks, run command below and review tasks under 'AEM' group:

```shell
gradlew tasks --all 
```

Sample output for project based on [AEM Project Archetype](https://github.com/adobe/aem-project-archetype)):

```

> Task :tasks

------------------------------------------------------------
Tasks runnable from root project 'mysite'
------------------------------------------------------------

AEM tasks
---------
env:instanceAwait - Await for healthy condition of all AEM instances.
env:instanceBackup - Turns off local instance(s), archives to ZIP file, then turns on again.
env:instanceCreate - Creates local AEM instance(s).
env:instanceDeploy - Deploys to instances package or bundle by providing URL, path or dependency notation
env:instanceDestroy - Destroys local AEM instance(s).
env:instanceDown - Turns off local AEM instance(s).
env:instanceGroovyEval - Evaluate Groovy script(s) on instance(s).
env:instanceKill - Kill local AEM instance process(es)
env:instanceProvision - Configures instances only in concrete circumstances (only once, after some time etc)
env:instanceRcp - Copy JCR content from one instance to another.
env:instanceReload - Reloads all AEM instance(s).
env:instanceResetup - Destroys then sets up local AEM instance(s).
env:instanceResolve - Resolves instance files from remote sources before running other tasks
env:instanceRestart - Turns off then on local AEM instance(s).
env:instanceSetup - Creates and turns on local AEM instance(s) with satisfied dependencies and application built.
env:instanceStatus - Prints status of AEM instances and installed packages.
env:instanceTail - Tails logs from all configured instances (local & remote) and notifies about unknown errors.
env:instanceUp - Turns on local AEM instance(s).
env:instanceWorkflow - Schedules workflow model on resources under the specified path
core:mvnJar - Builds JAR file
root:mvnPom - Installs POM to local repository
all:mvnZip - Builds CRX package
dispatcher:mvnZip - Builds ZIP archive
ui.apps:mvnZip - Builds CRX package
ui.apps.structure:mvnZip - Builds CRX package
ui.config:mvnZip - Builds CRX package
ui.content:mvnZip - Builds CRX package
ui.frontend:mvnZip - Builds AEM frontend
packageCleanAll - Cleans AEM modules built
all:packageConfig - Check out OSGi configuration then save as JCR content.
ui.apps:packageConfig - Check out OSGi configuration then save as JCR content.
ui.apps.structure:packageConfig - Check out OSGi configuration then save as JCR content.
ui.config:packageConfig - Check out OSGi configuration then save as JCR content.
ui.content:packageConfig - Check out OSGi configuration then save as JCR content.
all:packageDeploy - Deploys CRX package to instance
ui.apps:packageDeploy - Deploys CRX package to instance
ui.apps.structure:packageDeploy - Deploys CRX package to instance
ui.config:packageDeploy - Deploys CRX package to instance
ui.content:packageDeploy - Deploys CRX package to instance
packageDeployAll - Deploys CRX packages to instance
all:packageSync - Check out then clean JCR content.
ui.apps:packageSync - Check out then clean JCR content.
ui.apps.structure:packageSync - Check out then clean JCR content.
ui.config:packageSync - Check out then clean JCR content.
ui.content:packageSync - Check out then clean JCR content.

Build tasks
-----------
assemble - Assembles the outputs of this project.
all:assemble - Assembles the outputs of this project.
core:assemble - Assembles the outputs of this project.
dispatcher:assemble - Assembles the outputs of this project.
env:assemble - Assembles the outputs of this project.
root:assemble - Assembles the outputs of this project.
ui.apps:assemble - Assembles the outputs of this project.
ui.apps.structure:assemble - Assembles the outputs of this project.
ui.config:assemble - Assembles the outputs of this project.
ui.content:assemble - Assembles the outputs of this project.
ui.frontend:assemble - Assembles the outputs of this project.
build - Assembles and tests this project.
all:build - Assembles and tests this project.
core:build - Assembles and tests this project.
dispatcher:build - Assembles and tests this project.
env:build - Assembles and tests this project.
root:build - Assembles and tests this project.
ui.apps:build - Assembles and tests this project.
ui.apps.structure:build - Assembles and tests this project.
ui.config:build - Assembles and tests this project.
ui.content:build - Assembles and tests this project.
ui.frontend:build - Assembles and tests this project.
clean - Deletes the build directory.
all:clean - Deletes the build directory.
core:clean - Deletes the build directory.
dispatcher:clean - Deletes the build directory.
env:clean - Deletes the build directory.
root:clean - Deletes the build directory.
ui.apps:clean - Deletes the build directory.
ui.apps.structure:clean - Deletes the build directory.
ui.config:clean - Deletes the build directory.
ui.content:clean - Deletes the build directory.
ui.frontend:clean - Deletes the build directory.

Environment tasks
-----------------
env:environmentAwait - Await for healthy condition of environment.
env:environmentDestroy - Destroys virtualized environment.
env:environmentDev - Turns on environment development mode (interactive e.g HTTPD configuration reloading on file changes)
env:environmentDown - Turns off virtualized environment
env:environmentHosts - Updates environment hosts entries.
env:environmentReload - Reloads virtualized environment (e.g reloads HTTPD configuration and cleans cache files)
env:environmentResetup - Destroys then sets up virtualized development environment.
env:environmentResolve - Resolves environment files from remote sources before running other tasks
env:environmentRestart - Restart virtualized development environment.
env:environmentSetup - Sets up virtualized development environment.
env:environmentUp - Turns on virtualized environment

Runtime tasks
-------------
env:await - Await for healthy condition of all runtimes.
env:destroy - Destroys all runtimes.
env:down - Turns off all runtimes.
env:resetup - Destroys then sets up all runtimes.
env:resolve - Resolves all resources needed by all runtimes.
env:restart - Turns off then on all runtimes.
env:setup - Sets up all runtimes.
env:up - Turns on all runtimes.
```

Next steps to do after creating Gradle/GAP configuration i.e steps just done:

1. Run command `sh gradlew props` and specify AEM instance source files,
2. Run command `sh gradlew :env:setup` to set up complete AEM environment with building & deploying AEM application incrementally,
3. Run command `sh gradlew` (shorthand for `:env:setup`) to see that incremental build powered by Gradle/GAP is detecting no changes to apply :)
4. Run command `sh gradlew :deploy` to deploy only AEM packages to AEM instances without reloading AEM dispatcher.
 
### Setting up local instance

The procedure above will also work at the empty directory and could be used to set up AEM instances only.
It is using `sh gradlew props` task to provide AEM instance files details, however, such details could be also provided via the command line.

To set up and turn on AEM instance(s) by single command, consider running:

```bash
curl -OJL https://github.com/Cognifide/gradle-aem-plugin/releases/download/16.0.4/gap.jar \
&& java -jar gap.jar --save-props \
  -PfileTransfer.user=foo -PfileTransfer.password=pass \
  -PlocalInstance.quickstart.jarUrl=http://company-share.com/aem/cq-quickstart-6.5.0.jar \
  -PlocalInstance.quickstart.licenseUrl=http://company-share.com/aem/license.properties \
  -Pinstance.local-author.type=local \
&& rm gap.jar \
&& sh gradlew up
```

Once GAP is initialized and AEM instances are up, then to shut down instances use the command:

```shell
sh gradlew down
```

### Deploying packages

For deploying to AEM instance CRX package from any source consider using command:

```shell
curl -OJL https://github.com/Cognifide/gradle-aem-plugin/releases/download/16.0.4/gap.jar && java -jar gap.jar && rm gap.jar
sh gradlew instanceDeploy -Pinstance.author -Pinstance.deploy.packageUrl=https://github.com/neva-dev/felix-search-webconsole-plugin/releases/download/search-webconsole-plugin-1.3.0/search-webconsole-plugin-1.3.0.jar
```

Parameter `-Pinstance.author` is used to deploy only to default AEM author instance (available at *http://localhost:4502*), but any instances could be used, see [instance filtering](common-plugin.md#instance-filtering). 
Skip it to deploy package to both author & publish instances at once.

The URL could point to CRX package or to OSGi bundle which will be automatically wrapped into CRX package on-the-fly.

Notice that package URL could be using SMB/SFTP protocols too.
In such case remember to specify file transfer properties as in [local instance](#setting-up-local-instance) example.
Also instead of URL, dependency notation could be used to resolve package from Maven Central or JCenter repository.

### Tailing logs

To interactively monitor logs of any AEM instances using task [`instanceTail`](instance-plugin.md#task-instancetail), consider running command:

```bash
curl -OJL https://github.com/Cognifide/gradle-aem-plugin/releases/download/16.0.4/gap.jar \
&& java -jar gap.jar --save-props \
  -Pinstance.dev-author.httpUrl=http://foo:pass@10.11.12.1:4502 \
  -Pinstance.dev-publish.httpUrl=http://foo:pass@10.11.12.2:4503 \
&& rm gap.jar
sh gradlew instanceTail
```

### Syncing content

To pull JCR content with content normalization from running instance using task [`packageSync`](package-sync-plugin.md), consider running command:
Assuming instance running at URL *http://localhost:4502* or *http://localhost:4503*. 
Consider appending parameter e.g `-Pinstance.list=http://admin:admin@localhost:4502` to customize the instance to work with.

```bash
curl -OJL https://github.com/Cognifide/gradle-aem-plugin/releases/download/16.0.4/gap.jar && java -jar gap.jar && rm gap.jar
sh gradlew packageSync -Pfilter.roots=[/content/example,/content/dam/example]
```

### Copying content between instances

To copy JCR content between any AEM instances using task [`instanceRcp`](instance-plugin.md#task-instancercp), consider running commands:

```bash
curl -OJL https://github.com/Cognifide/gradle-aem-plugin/releases/download/16.0.4/gap.jar && java -jar gap.jar && rm gap.jar
sh gradlew instanceRcp \
  -Pinstance.rcp.source=http://foo:pass@10.11.12.1:4502 \
  -Pinstance.rcp.target=http://foo:pass@10.11.12.2:4503 \
  -Pinstance.rcp.paths=[/content/example,/content/dam/example]
```

## Options

### Saving properties

Note that when it is needed to e.g specify GAP properties e.g related with source of AEM instance JAR & license files when running `up` task, 
consider adding argument `--save-props` when running GAP launcher. It will save all other command line properties to `gradle.properties` file.
Thanks to that, when running `down` task next time, all properties related with instance definitions will be no longer needed to be passed as command line arguments.

Alternatively, when technique for credentials passed as command line parameters is considered as not enough safe, it is an option to create file `gradle.properties` 
and specify all required properties there before running the launcher.

### Console output

Gradle rich console output may not work properly on all environments. To disable rich color output, add parameters `--no-color -i` to enforce plain text output.

### Gradle installation options

Customizing [gradle wrapper properties](https://docs.gradle.org/7.5.1/userguide/gradle_wrapper.html#sec:adding_wrapper) is possible by passing in `--gradle-version`, `--distribution-type`, `--gradle-distribution-url`, or `--gradle-distribution-sha256-sum` arguments to `java -jar gap.jar`.
Note that these are only applied the first time `gap.jar` is run in a directory and ignored on subsequent invocations.
