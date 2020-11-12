#!/bin/sh

(cd {{ rootProject.projectDir }} && {{ instance.serviceOpts['environmentCommand'] }} && {{ instance.serviceOpts['startCommand'] }} -Pinstance.name={{ instance.name }})