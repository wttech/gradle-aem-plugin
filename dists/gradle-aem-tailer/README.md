# Gradle AEM Tailer

Tailer is based on Gradle AEM Plugin task named `aemTail`. See [documentation](../../README.md#task-aemtail).

## Usage

1. Download archive [gradle-aem-tailer.zip](https://github.com/Cognifide/gradle-aem-plugin/raw/master/dists/gradle-aem-tailer.zip)
2. Extract archive on any file system location.
3. Start tool:
    * Windows - script: *gradlew.bat* (by double clicking)
    * Unix - script: *gradlew*
4. Use *Ctrl + C* to stop tool.

## Configuration

Instance URLs can be configured in file *gradle.properties*.
To skip incident reports for known issues, add log text exclusion rules to file *incidentFilter.txt*.