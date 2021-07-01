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

## Downloads

Grab most recent version of launcher from GitHub [releases](https://github.com/Cognifide/gradle-aem-plugin/releases) section.

The launcher on release asset list is a file named **gap.jar**.

## Usages

Below there are some sample usages of standalone launcher.

### Enhancing Maven build

To add Gradle/GAP support to existing Maven build generated from Adobe AEM Archetype, run command below:

```shell
curl -OJL https://github.com/Cognifide/gradle-aem-plugin/releases/download/15.1.24/gap.jar && java -jar gap.jar && rm gap.jar
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

Sample output (for [AEM Guides WKND](https://github.com/adobe/aem-guides-wknd)):

```
AEM tasks
---------
all:config - Check out OSGi configuration then save as JCR content.
ui.apps:config - Check out OSGi configuration then save as JCR content.
ui.apps.structure:config - Check out OSGi configuration then save as JCR content.
ui.config:config - Check out OSGi configuration then save as JCR content.
ui.content:config - Check out OSGi configuration then save as JCR content.
ui.content.sample:config - Check out OSGi configuration then save as JCR content.
all:deploy - Deploys AEM package to instance
ui.apps:deploy - Deploys AEM package to instance
ui.apps.structure:deploy - Deploys AEM package to instance
ui.config:deploy - Deploys AEM package to instance
ui.content:deploy - Deploys AEM package to instance
ui.content.sample:deploy - Deploys AEM package to instance
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
core:jar - Builds JAR file
it.tests:jar - Builds JAR file
ui.tests:module - Builds module
root:pom - Installs POM to local repository
all:sync - Check out then clean JCR content.
ui.apps:sync - Check out then clean JCR content.
ui.apps.structure:sync - Check out then clean JCR content.
ui.config:sync - Check out then clean JCR content.
ui.content:sync - Check out then clean JCR content.
ui.content.sample:sync - Check out then clean JCR content.
all:zip - Builds AEM package
dispatcher.cloud:zip - Builds ZIP archive
ui.apps:zip - Builds AEM package
ui.apps.structure:zip - Builds AEM package
ui.config:zip - Builds AEM package
ui.content:zip - Builds AEM package
ui.content.sample:zip - Builds AEM package
ui.frontend:zip - Builds AEM frontend
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
curl -OJL https://github.com/Cognifide/gradle-aem-plugin/releases/download/15.0.4/gap.jar \
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
curl -OJL https://github.com/Cognifide/gradle-aem-plugin/releases/download/15.0.4/gap.jar && java -jar gap.jar && rm gap.jar
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
curl -OJL https://github.com/Cognifide/gradle-aem-plugin/releases/download/15.0.4/gap.jar \
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
curl -OJL https://github.com/Cognifide/gradle-aem-plugin/releases/download/15.0.4/gap.jar && java -jar gap.jar && rm gap.jar
sh gradlew packageSync -Pfilter.roots=[/content/example,/content/dam/example]
```

### Copying content between instances

To copy JCR content between any AEM instances using task [`instanceRcp`](instance-plugin.md#task-instancercp), consider running commands:

```bash
curl -OJL https://github.com/Cognifide/gradle-aem-plugin/releases/download/15.0.4/gap.jar && java -jar gap.jar && rm gap.jar
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
