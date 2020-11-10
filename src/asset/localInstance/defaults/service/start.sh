#!/bin/sh

(cd {{ rootProject.projectDir }} && {{ instance.serviceOpts['startCommand'] }} -Pinstance.name={{ instance.name }})