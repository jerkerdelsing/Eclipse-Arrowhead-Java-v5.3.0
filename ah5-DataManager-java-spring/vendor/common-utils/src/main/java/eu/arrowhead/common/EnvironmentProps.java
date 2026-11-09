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

	private static ApplicationContext context;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void setApplicationContext(final ApplicationContext ctx) {
		context = ctx;
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	public static Environment getEnvironment() {
		while (context == null) {
			try {
				Thread.sleep(100);
			} catch (final InterruptedException ex) {
				// intentionally blank
			}
		}

		return context.getEnvironment();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	public static String getProperty(final String key) {
		while (context == null) {
			try {
				Thread.sleep(100);
			} catch (final InterruptedException ex) {
				// intentionally blank
			}
		}

		return context.getEnvironment().getProperty(key);
	}
}