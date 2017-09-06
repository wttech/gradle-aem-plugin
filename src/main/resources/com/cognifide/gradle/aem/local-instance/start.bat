set CQ_RUNMODE={{instance.typeName}},local
set CQ_PORT={{instance.httpPort}}
set CQ_JVM_OPTS=-Xmx1024m -XX:MaxPermSize=256M -Djava.awt.headless=true -Xdebug -Xrunjdwp:transport=dt_socket,address={{instance.debugPort}},server=y,suspend=n

call crx-quickstart/bin/start.bat