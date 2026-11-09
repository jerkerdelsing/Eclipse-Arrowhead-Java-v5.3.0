package eu.arrowhead.localcloudmanager.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.localcloudmanager.LocalCloudManagerConstants;

/** Docker is reached only through the allowlisting Docker proxy on an internal network; LocalCloudManager never holds the socket. */
@Component
public class DockerGateway {

	private final URI base;
	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();

	public DockerGateway(@Value(LocalCloudManagerConstants.$DOCKER_URL_WD) final String url) {
		this.base = URI.create(url);
	}

	public byte[] request(final String path, final String method) throws Exception {
		final HttpRequest request = HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(30))
				.method(method, HttpRequest.BodyPublishers.noBody()).build();
		final HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
		if (response.statusCode() >= 400) {
			final String text = new String(response.body(), StandardCharsets.UTF_8);
			throw new IllegalStateException("Docker API " + response.statusCode() + ": " + text.substring(0, Math.min(300, text.length())));
		}
		return response.body();
	}
}
