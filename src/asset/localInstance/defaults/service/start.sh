#!/bin/sh

(cd {{ rootProject.projectDir }} && sh gradlew -i --console=plain instanceUp -Pinstance.name={{ instance.name }})