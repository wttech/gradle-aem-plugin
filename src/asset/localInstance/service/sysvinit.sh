#!/bin/bash
### BEGIN INIT INFO
# Provides:          aem
# Required-Start:    $local_fs $remote_fs $network $syslog $named
# Required-Stop:     $local_fs $remote_fs $network $syslog $named
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Start AEM instances
# Description:       Init script for AEM instances
### END INIT INFO

. /lib/lsb/init-functions

NAME=aem
AEM_USER={{ service.opts['user'] }}

START={{ service.opts['dir'] }}/start.sh
STOP={{ service.opts['dir'] }}/stop.sh
STATUS={{ service.opts['dir'] }}/status.sh

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