#!/bin/sh

(cd {{ rootProject.projectDir }} && {{ instance.serviceOpts['environmentCommand'] }} && {{ instance.serviceOpts['statusCommand'] }} -Pinstance.name={{ instance.name }})