#!/bin/sh

(cd {{ rootProject.projectDir }} && {{ instance.serviceOpts['stopCommand'] }} -Pinstance.name={{ instance.name }})