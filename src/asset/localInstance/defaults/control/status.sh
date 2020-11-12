#!/bin/sh

export JAVA_EXECUTABLE='{{ java.executablePath }}'

chmod u+x crx-quickstart/bin/status
./crx-quickstart/bin/status