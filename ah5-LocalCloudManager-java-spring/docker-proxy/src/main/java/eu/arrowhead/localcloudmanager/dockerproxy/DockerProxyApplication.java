package eu.arrowhead.localcloudmanager.dockerproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Docker Engine API proxy for the LocalCloudManager console (security finding H1). The console API does not mount
 * /var/run/docker.sock; it reaches Docker only through this separate process, on an internal network that nothing else joins.
 */
@SpringBootApplication
public class DockerProxyApplication {

	public static void main(final String[] args) {
		SpringApplication.run(DockerProxyApplication.class, args);
	}
}
