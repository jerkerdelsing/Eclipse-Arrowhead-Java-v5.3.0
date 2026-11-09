/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.common.init;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;

// Turns a failed start-up into a process exit. Some libraries leave non-daemon threads behind when the Spring
// context fails (for example an MQTT reconnect timer); the JVM would then keep running without serving, and the
// container restart policy would never retry. If the JVM has not ended on its own after a grace period, it exits
// with status 1. Disabled with -Darrowhead.startup.failure.exit=false and inside test runners.
public class StartupFailureExit implements ApplicationListener<ApplicationFailedEvent> {

	public static final String PROPERTY = "arrowhead.startup.failure.exit";
	static final long GRACE_PERIOD_MS = 10_000;

	private final Logger logger = LogManager.getLogger(StartupFailureExit.class);

	@Override
	public void onApplicationEvent(final ApplicationFailedEvent event) {
		if (!enabled()) {
			return;
		}

		final Thread watchdog = new Thread(() -> {
			try {
				Thread.sleep(GRACE_PERIOD_MS);
			} catch (final InterruptedException ex) {
				Thread.currentThread().interrupt();
				return;
			}
			logger.error("Start-up failed and the JVM is still running; exiting with status 1");
			System.exit(1);
		}, "startup-failure-exit");
		watchdog.setDaemon(true); // does not keep the JVM alive when it would end anyway
		watchdog.start();
	}

	static boolean enabled() {
		if (!Boolean.parseBoolean(System.getProperty(PROPERTY, "true"))) {
			return false;
		}
		// never terminate a test runner's JVM
		return System.getProperty("surefire.test.class.path") == null && System.getProperty("surefire.real.class.path") == null;
	}
}
