CQ_PORT=${instance.httpPort}
CQ_RUNMODE='${instance.typeName},local'
CQ_JVM_OPTS='-server -Xmx1024m -XX:MaxPermSize=256M -Djava.awt.headless=true -Xdebug -Xrunjdwp:transport=dt_socket,address=${instance.debugPort},server=y,suspend=n'

source crx-quickstart/bin/start