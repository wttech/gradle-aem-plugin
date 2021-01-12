set JAVA_EXECUTABLE="{{ instance.javaExecutablePath }}"

set CQ_PORT={{instance.httpPort}}
set CQ_RUNMODE={{instance.runModesString}}
set CQ_JVM_OPTS={{instance.jvmOptsString}}
set CQ_START_OPTS={{instance.startOptsString}}

call crx-quickstart\bin\start.bat