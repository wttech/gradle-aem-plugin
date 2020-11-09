#!/bin/bash
### BEGIN INIT INFO
# Provides:          aem_{{ instance.id }}
# Required-Start:    $local_fs $remote_fs $network $syslog $named
# Required-Stop:     $local_fs $remote_fs $network $syslog $named
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Start AEM process
# Description:       Init script for AEM instance {{ instance.id }}
### END INIT INFO

. /lib/lsb/init-functions

NAME=aem_{{ instance.id }}
AEM_USER={{ instance.serviceOpts['user'] }}
PID_PATH={{ instance.pidFile }}

START={{ instance.dir }}/service/start.sh
STOP={{ instance.dir }}/service/stop.sh
STATUS={{ instance.dir }}/service/status.sh

aem_start() {
  pidResult=$(pgrep --pidfile "$PID_PATH" || true);
  if [[ $pidResult != "" ]];
  then
    log_success_msg "Already started: $NAME"
  else
    log_daemon_msg "Starting: $NAME"
    su - "${AEM_USER}" -c "sh ${START}"
    log_end_msg 0
  fi
}

aem_stop() {
  su - "${AEM_USER}" -c "sh ${STOP}"
}

aem_status() {
  su - "${AEM_USER}" -c "sh ${STATUS}"
}

case "$1" in
  start)
    aem_start
  ;;
  stop)
    aem_stop
   ;;
  status)
    aem_status
  ;;
  restart)
    aem_stop
    aem_start
  ;;
  *)
    echo "Usage: $NAME {start|stop|status|restart}"
  exit 1
  ;;
esac