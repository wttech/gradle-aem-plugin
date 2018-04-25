set CQ_PORT={{instance.httpPort}}
set CQ_RUNMODE={{instance.runModesString}}
set CQ_JVM_OPTS={{instance.jvmOptsString}}
set START_OPTS=start -c {{handle.staticDir}} -i launchpad {{instance.startOptsString}}

call crx-quickstart\bin\start.bat