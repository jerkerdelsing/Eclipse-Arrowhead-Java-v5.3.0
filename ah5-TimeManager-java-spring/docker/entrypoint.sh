#!/bin/sh
set -e

if [ -d "/app/config_temp" ] && [ "$(ls -A /app/config_temp)" ]; then
    cp -r /app/config_temp/* /app/config/
    rm -rf /app/config_temp
fi

exec java -Dlog4j.configurationFile=/app/config/log4j2.xml -jar "/app/arrowhead-timemanager-${AH_VERSION}.jar"
