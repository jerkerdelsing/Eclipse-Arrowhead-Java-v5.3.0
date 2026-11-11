# Ontotext GraphDB Free for GraphDBManager: the historian repository, an admin password and the system's own
# user are set up on start by entrypoint.sh (idempotent). Runs as UID 10001; data in /opt/graphdb/home.
FROM ontotext/graphdb:10.8.14@sha256:506096f8b30c44994d457551f668fa1283596cfc7098bdb915f9b6b708ec2f44
# SOS-16 (2026-11-10): Ubuntu security updates on top of the pinned base image
RUN apt-get update && apt-get -y upgrade && rm -rf /var/lib/apt/lists/*
COPY deployment/graphdb/entrypoint.sh deployment/graphdb/repository-config.ttl /opt/graphdbmanager/
RUN chmod 755 /opt/graphdbmanager/entrypoint.sh && mkdir -p /opt/graphdb/home && chown -R 10001:10001 /opt/graphdb/home
USER 10001:10001
EXPOSE 7200
HEALTHCHECK --interval=10s --start-period=120s --timeout=4s CMD ["test", "-f", "/tmp/graphdb-ready"]
ENTRYPOINT ["/opt/graphdbmanager/entrypoint.sh"]
