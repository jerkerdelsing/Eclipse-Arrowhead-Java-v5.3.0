#!/bin/sh
# The certificate keys are readable by root only; hand the broker its own copy.
set -eu
mkdir -p /etc/rabbitmq/tls
cp /certs/ca.crt /certs/amqp-broker.crt /certs/amqp-broker.key /etc/rabbitmq/tls/
chown -R rabbitmq:rabbitmq /etc/rabbitmq/tls
chmod 600 /etc/rabbitmq/tls/amqp-broker.key
exec docker-entrypoint.sh rabbitmq-server
