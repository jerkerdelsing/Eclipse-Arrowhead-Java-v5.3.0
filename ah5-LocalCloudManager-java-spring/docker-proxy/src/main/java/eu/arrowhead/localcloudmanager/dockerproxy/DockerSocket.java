package eu.arrowhead.localcloudmanager.dockerproxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;

/** A minimal HTTP/1.0 client for the Docker Engine API on its Unix socket. */
public class DockerSocket {

	public record Response(int status, String contentType, byte[] body) {
	}

	private static final int MAX_BYTES = 16 << 20;

	private final Path socket;

	public DockerSocket(final Path socket) {
		this.socket = socket;
	}

	public Response request(final String method, final String path) throws IOException {
		try (SocketChannel channel = SocketChannel.open(UnixDomainSocketAddress.of(socket))) {
			final OutputStream out = Channels.newOutputStream(channel);
			out.write((method + " " + path + " HTTP/1.0\r\nHost: docker\r\nContent-Length: 0\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
			out.flush();
			final InputStream in = Channels.newInputStream(channel);
			final byte[] raw = readAll(in);
			int end = -1;
			for (int i = 0; i + 3 < raw.length; i++) {
				if (raw[i] == '\r' && raw[i + 1] == '\n' && raw[i + 2] == '\r' && raw[i + 3] == '\n') {
					end = i;
					break;
				}
			}
			if (end < 0) {
				throw new IOException("Invalid Docker response");
			}
			final String[] head = new String(raw, 0, end, StandardCharsets.ISO_8859_1).split("\r\n");
			final int status = Integer.parseInt(head[0].split(" ")[1]);
			String type = "application/octet-stream";
			boolean chunked = false;
			for (int i = 1; i < head.length; i++) {
				final String line = head[i].toLowerCase(Locale.ROOT);
				if (line.startsWith("content-type:")) {
					type = head[i].substring(13).trim();
				}
				if (line.startsWith("transfer-encoding:") && line.contains("chunked")) {
					chunked = true;
				}
			}
			byte[] body = java.util.Arrays.copyOfRange(raw, end + 4, raw.length);
			if (chunked) {
				body = dechunk(body);
			}
			return new Response(status, type, body);
		}
	}

	private static byte[] readAll(final InputStream in) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final byte[] buffer = new byte[8192];
		int n;
		while ((n = in.read(buffer)) >= 0) {
			out.write(buffer, 0, n);
			if (out.size() > MAX_BYTES) {
				throw new IOException("Docker response too large");
			}
		}
		return out.toByteArray();
	}

	static byte[] dechunk(final byte[] data) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		int i = 0;
		while (i < data.length) {
			int lineEnd = i;
			while (lineEnd + 1 < data.length && !(data[lineEnd] == '\r' && data[lineEnd + 1] == '\n')) {
				lineEnd++;
			}
			final String sizeText = new String(data, i, lineEnd - i, StandardCharsets.US_ASCII).split(";")[0].trim();
			final int size = sizeText.isEmpty() ? 0 : Integer.parseInt(sizeText, 16);
			if (size == 0) {
				break;
			}
			out.write(data, lineEnd + 2, Math.min(size, data.length - lineEnd - 2));
			i = lineEnd + 2 + size + 2;
		}
		return out.toByteArray();
	}
}
