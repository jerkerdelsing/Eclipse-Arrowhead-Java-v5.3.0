FROM mysql:8.4.11
COPY deployment/database/init.sql /docker-entrypoint-initdb.d/10-schema.sql
COPY deployment/database/least-privilege.sh /docker-entrypoint-initdb.d/20-least-privilege.sh
