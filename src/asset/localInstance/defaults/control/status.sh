#!/bin/sh

export JAVA_EXECUTABLE='{{ instance.javaExecutablePath }}'

chmod u+x crx-quickstart/bin/status
./crx-quickstart/bin/status