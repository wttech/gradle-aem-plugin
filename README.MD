![Cognifide logo](docs/cognifide-logo.png)

[![Gradle Status](https://gradleupdate.appspot.com/Cognifide/gradle-sling-plugin/status.svg)](https://gradleupdate.appspot.com/Cognifide/gradle-sling-plugin/status)
[![Apache License, Version 2.0, January 2004](docs/apache-license-badge.svg)](http://www.apache.org/licenses/)
![Travis Build](https://travis-ci.org/Cognifide/gradle-sling-plugin.svg?branch=develop)

# Gradle Sling Plugin

<p align="center">
  <img src="docs/gsp-logo.png" alt="Logo"/>
</p>

## Description

Currently there is no popular way to build applications for Sling using Gradle build system. This project contains brand new Gradle plugin to assemble Vault package and deploy it on instance(s).

Incremental build which takes seconds, not minutes. Developer who does not loose focus between build time gaps. Extend freely your build system directly in project. 

Sling developer - it's time to meet Gradle! You liked or used plugin? Don't forget to **star this project** on GitHub :)

Check out [live demo](https://adapt.to/2018/en/schedule/a-better-developer-experience-for-sling-based-applications.html) presented on official **Sling adaptTo() 2018** conference.

## Documentation

This project is a **fork** of [Gradle AEM Plugin](https://github.com/Cognifide/gradle-aem-plugin).

Almost all of its concepts are applicable to pure Sling so that all of features of GAP 4.x.x are available in GSP.
The maintenance and synchronization of two separate plugins may be exhausting, so that any **volunteers** that will take care about this fork are appreciated.
Generally to keep documentation up to date and occasionally transfer code from GAP to GSP and vice versa.

For now, just consider mapping word **aem** to **sling** while reading GAP documentation to be able to start work on GSP.
This is also applicable to build script, for instance:

```groovy
sling {
    // ...
}
```

Task names are not changed - are same as in Gradle AEM Plugin.

## Building

1. Clone this project using command `git clone https://github.com/Cognifide/gradle-sling-plugin.git`
2. Enter cloned directory and simply run command: `gradlew`
3. To use built plugin:
    * Add `mavenLocal()` to `repositories` section inside `pluginManagement` of *settings.gradle* file.
    * Ensuring having correct version of plugin specified in *settings.gradle* file.
4. To debug built plugin:
    * Append to build command parameters `--no-daemon -Dorg.gradle.debug=true`
    * Run build, it will suspend, then connect remote at port 5005 by using IDE
    * Build will proceed and stop at previously set up breakpoint.
    
## Contributing

Issues reported or pull requests created will be very appreciated. 

1. Fork plugin source using a dedicated GitHub button.
2. Do code changes on a feature branch created from *develop* branch.
3. Create a pull request with a base of *develop* branch.

## License

**Gradle Sling Plugin** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)