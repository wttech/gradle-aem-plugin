#!/bin/sh

export JAVA_EXECUTABLE='{{ java.executablePath }}'

chmod u+x crx-quickstart/bin/stop
./crx-quickstart/bin/stop