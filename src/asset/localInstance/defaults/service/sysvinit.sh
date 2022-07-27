#!/bin/bash
### BEGIN INIT INFO
# Provides:          aem_{{ instance.purposeId }}
# Required-Start:    $local_fs $remote_fs $network $syslog $named
# Required-Stop:     $local_fs $remote_fs $network $syslog $named
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Start AEM instance
# Description:       Init script for AEM instance {{ instance.purposeId }}
### END INIT INFO

. /lib/lsb/init-functions

NAME=aem_{{ instance.purposeId }}
AEM_USER={{ service.opts['user'] }}

START={{ instance.dir }}/service/start.sh
STOP={{ instance.dir }}/service/stop.sh
STATUS={{ instance.dir }}/service/status.sh

aem_start() {
  su - "${AEM_USER}" -c "sh ${START}"
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