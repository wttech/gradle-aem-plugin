#!/bin/sh

export CQ_PORT={{instance.httpPort}}
export CQ_RUNMODE='{{instance.runModesString}}'
export CQ_JVM_OPTS='{{instance.jvmOptsString}}'
export CQ_START_OPTS='{{instance.startOptsString}}'

chmod u+x crx-quickstart/bin/start
./crx-quickstart/bin/start