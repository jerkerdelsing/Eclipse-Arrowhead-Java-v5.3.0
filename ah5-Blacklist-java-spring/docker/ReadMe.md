**Expected folder structure:**

```
root-folder
	└── ah5-common-java-spring/
	└── ah5-blacklist-java-spring/
```

# BUILD IMAGES

1) Run the system build command from the _root-folder_. Make sure you use the proper version number for build arg and also for the tag!

`docker build -f ./ah5-blacklist-java-spring/docker/Dockerfile-Blacklist -t arrowhead-blacklist:5.0.0 --build-arg AH_VERSION=5.0.0 .`

2) Run the database build command from the _root-folder_. Make sure you use the proper version number for the tag.

`docker build -f ./ah5-blacklist-java-spring/docker/Dockerfile-Blacklist-DB -t arrowhead-blacklist-db:5.0.0 .`