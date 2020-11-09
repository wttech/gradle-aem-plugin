#!/bin/sh

(cd {{ rootProject.projectDir }} && sh gradlew -q --console=plain instanceStatus -Pinstance.name={{ instance.name }})