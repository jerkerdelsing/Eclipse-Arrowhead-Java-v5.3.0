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
package eu.arrowhead.common.init;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;

/**
 * Refuses to start a system whose effective configuration falls below the default secure profile of
 * the architecture (full AA and encrypted information exchange, since v5.0), unless the deployment
 * selects the isolated development exception with arrowhead.security.profile=development.
 *
 * It runs as a bean factory post processor, before any singleton exists. A later check would come
 * too late: ContextRefreshedEvent is published after the embedded web server has started, and a
 * bean-level check could be hidden by another bean that fails first, such as the database.
 */
@Component
public class SecurityProfileGuard implements BeanFactoryPostProcessor, EnvironmentAware {

	//=================================================================================================
	// members

	public static final String PROFILE_SECURE = "secure";
	public static final String PROFILE_DEVELOPMENT = "development";

	private static final String SERVICE_AUTHORIZATION_ENABLED = "service.authorization.enabled";

	private Environment environment;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) {
		enforce();
	}

	//-------------------------------------------------------------------------------------------------
	public void enforce() {
		logger.debug("enforce started...");

		final String profile = environment.getProperty(Constants.ARROWHEAD_SECURITY_PROFILE, Defaults.ARROWHEAD_SECURITY_PROFILE_DEFAULT);
		final String normalizedProfile = profile.trim().toLowerCase(Locale.ROOT);
		if (!PROFILE_SECURE.equals(normalizedProfile) && !PROFILE_DEVELOPMENT.equals(normalizedProfile)) {
			throw new IllegalStateException(Constants.ARROWHEAD_SECURITY_PROFILE + " must be '" + PROFILE_SECURE + "' or '" + PROFILE_DEVELOPMENT + "', not '" + profile + "'");
		}

		final List<String> deviations = findDeviations();
		if (deviations.isEmpty()) {
			logger.info("Security profile: secure");
			return;
		}

		if (PROFILE_SECURE.equals(normalizedProfile)) {
			throw new IllegalStateException("Refusing to start: the configuration does not meet the default secure profile: " + String.join("; ", deviations)
					+ ". Provide the secure configuration of the deployment. For an isolated test only, select " + Constants.ARROWHEAD_SECURITY_PROFILE + "=" + PROFILE_DEVELOPMENT + " explicitly.");
		}

		logger.warn("NON-CONFORMING development exception selected for an isolated test; do not expose this system: {}", String.join("; ", deviations));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<String> findDeviations() {
		final List<String> deviations = new ArrayList<>();

		if (!environment.getProperty(Constants.SERVER_SSL_ENABLED, Boolean.class, Boolean.valueOf(Defaults.SERVER_SSL_ENABLED_DEFAULT))) {
			deviations.add(Constants.SERVER_SSL_ENABLED + "=false");
		}

		final String policy = environment.getProperty(Constants.AUTHENTICATION_POLICY, Defaults.AUTHENTICATION_POLICY_DEFAULT);
		if (AuthenticationPolicy.DECLARED.name().equalsIgnoreCase(policy.trim())) {
			deviations.add(Constants.AUTHENTICATION_POLICY + "=declared");
		}

		// not every system has this property; only an explicit false disables enforcement
		if (Boolean.FALSE.equals(environment.getProperty(SERVICE_AUTHORIZATION_ENABLED, Boolean.class))) {
			deviations.add(SERVICE_AUTHORIZATION_ENABLED + "=false");
		}

		final boolean monitorEnabled = environment.getProperty(Constants.SYS_MONITOR_ENABLED, Boolean.class, false);
		if (monitorEnabled && Boolean.FALSE.equals(environment.getProperty(Constants.SYS_MONITOR_SSL_ENABLED, Boolean.class))) {
			deviations.add(Constants.SYS_MONITOR_SSL_ENABLED + "=false");
		}

		return deviations;
	}
}
