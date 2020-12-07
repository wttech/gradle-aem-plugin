#!/bin/sh

(cd {{ rootProject.projectDir }} && {{ service.opts['environmentCommand'] }} && {{ service.opts['startCommand'] }})