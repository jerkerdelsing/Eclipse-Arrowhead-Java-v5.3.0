/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/

package eu.arrowhead.common;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentProps implements ApplicationContextAware {

	//=================================================================================================
	// members

	private static volatile ApplicationContext context;
	private static volatile boolean startupFailed = false;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void setApplicationContext(final ApplicationContext ctx) {
		context = ctx;
	}

	//-------------------------------------------------------------------------------------------------
	public static Environment getEnvironment() {
		awaitContext();

		return context.getEnvironment();
	}

	//-------------------------------------------------------------------------------------------------
	public static String getProperty(final String key) {
		awaitContext();

		return context.getEnvironment().getProperty(key);
	}

	//-------------------------------------------------------------------------------------------------
	// The context will never arrive after a failed startup. Without this, the database log appender
	// would wait here while it holds the lock that the JVM needs for its shutdown.
	public static void startupFailed() {
		startupFailed = true;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	private static void awaitContext() {
		while (context == null) {
			if (startupFailed) {
				throw new IllegalStateException("Startup failed before the environment was available");
			}

			try {
				Thread.sleep(100);
			} catch (final InterruptedException ex) {
				// intentionally blank
			}
		}
	}
}