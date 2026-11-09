package eu.arrowhead.localcloudmanager.dockerproxy;

/** A request to forward (method and Docker path) or a refusal (status and error). */
public record Decision(String method, String path, int status, String error) {

	public static Decision forward(final String method, final String path) {
		return new Decision(method, path, 0, null);
	}

	public static Decision refuse(final int status, final String error) {
		return new Decision(null, null, status, error);
	}

	public boolean refused() {
		return status != 0;
	}
}
