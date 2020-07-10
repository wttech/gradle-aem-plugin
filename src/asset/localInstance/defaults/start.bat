:: TODO pass it via sling.properties
set CQ_PORT={{instance.httpPort}}
set CQ_RUNMODE={{instance.runModesString}}
set CQ_JVM_OPTS={{instance.jvmOptsString}}
set CQ_START_OPTS={{instance.startOptsString}}

call sling\bin\start.bat