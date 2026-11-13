**Expected folder structure:**

```
root-folder
	└── ah5-common-java-spring/
	└── ah5-core-java-spring/
```

# BUILD IMAGES

1) Run the system build command from the _root-folder_. Make sure you use the proper version number for build arg and also for the tag!

`docker build -f ./ah5-core-java-spring/consumer-authorization/docker/Dockerfile-ConsumerAuthorization -t arrowhead-consumerauthorization:5.3.0 --build-arg AH_VERSION=5.3.0 .`

Since release 5.3.0 the system runs as UID/GID 10001, not as root. If you mount a volume or host folder at `/app/config`, it must be writable by UID 10001 (the entrypoint copies the default configuration into it on first start).

2) Run the database build command from the _root-folder_ (MySQL 8.4 LTS since release 5.3.0). Make sure you use the proper version number for the tag. Start the container with `AH_DB_USER` and `AH_DB_USER_PASSWORD` (or `AH_DB_USER_PASSWORD_FILE`) so that the system's database user does not keep the published default password.

`docker build -f ./ah5-core-java-spring/consumer-authorization/docker/Dockerfile-ConsumerAuthorization-DB -t arrowhead-consumerauthorization-db:5.3.0 .`