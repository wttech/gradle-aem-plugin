#!/bin/sh

export JAVA_EXECUTABLE='{{ instance.javaExecutablePath }}'

chmod u+x crx-quickstart/bin/stop
./crx-quickstart/bin/stop