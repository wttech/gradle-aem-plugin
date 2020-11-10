#!/bin/sh

(cd {{ rootProject.projectDir }} && {{ instance.serviceOpts['statusCommand'] }} -Pinstance.name={{ instance.name }})