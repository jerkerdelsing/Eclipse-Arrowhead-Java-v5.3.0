package eu.arrowhead.localcloudconfigurator;

/** A request the configurator refuses with a specific HTTP status and message (formerly ApiServer.Refused). */
public final class Refused extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int status;

	public Refused(final int status, final String message) {
		super(message);
		this.status = status;
	}

	public int status() {
		return status;
	}
}
