**Expected folder structure:**

```
root-folder
	└── ah5-common-java-spring/
	└── ah5-core-java-spring/
```

# BUILD IMAGES

1) Run the system build command from the _root-folder_. Make sure you use the proper version number for build arg and also for the tag!

`docker build -f ./ah5-core-java-spring/serviceorchestration-simple/docker/Dockerfile-SimpleStoreServiceOrchestration -t arrowhead-serviceorchestration-simple:5.2.2 --build-arg AH_VERSION=5.2.2 .`

2) Run the database build command from the _root-folder_. Make sure you use the proper version number for the tag.

`docker build -f ./ah5-core-java-spring/serviceorchestration-simple/docker/Dockerfile-SimpleStoreServiceOrchestration-DB -t arrowhead-serviceorchestration-simple-db:5.2.2 .`

# PORT AND SECURITY PROFILE

An image built from this tree listens on port 8442, the value in `application.properties`. The `SERVER_PORT=8442` override that deployments needed for the upstream image, which still carries 8465, is not needed here.

The `application.properties` packaged in the image is the development sample, so a container started with it stops with a clear error. Mount the secure configuration of the deployment, or set the matching environment variables. See [SECURITY-PROFILE.md](../../../SECURITY-PROFILE.md).
