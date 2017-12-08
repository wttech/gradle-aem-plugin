#!/bin/bash

export CQ_PORT={{instance.httpPort}}
export CQ_RUNMODE='{{instance.typeName}},local'
export CQ_JVM_OPTS='-server -Xmx1024m -XX:MaxPermSize=256M -Dadmin.password.file=password.properties -Djava.awt.headless=true -Xdebug -Xrunjdwp:transport=dt_socket,address={{instance.debugPort}},server=y,suspend=n {{instance.jvmOpt}}'
export START_OPTS='start -c ${CURR_DIR} -i launchpad -nointeractive {{instance.startOpt}}'

chmod u+x crx-quickstart/bin/start
./crx-quickstart/bin/start