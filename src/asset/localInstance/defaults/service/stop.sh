#!/bin/sh

(cd {{ rootProject.projectDir }} && sh gradlew -i --console=plain instanceDown -Pinstance.name={{ instance.name }})