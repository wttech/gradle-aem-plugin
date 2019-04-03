#!/bin/bash

printf "%-15s: %s %s\n" $1 $2 $3 >> /opt/aem/dispatcher/logs/invalidate.log

exit 0
