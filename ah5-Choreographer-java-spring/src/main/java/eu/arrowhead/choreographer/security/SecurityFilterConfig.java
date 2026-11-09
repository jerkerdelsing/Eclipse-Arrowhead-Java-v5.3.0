/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.filter.authentication.IAuthenticationPolicyFilter;
import jakarta.servlet.Filter;

// Replaces the servlet registration of the common authentication policy filter with OrchestrationCallbackFilter,
// which delegates to it for every path except the DynamicServiceOrchestration push callback.
@Configuration
@ConditionalOnExpression("'" + Constants.$AUTHENTICATION_POLICY_WD + "' != '" + AuthenticationPolicy.INTERNAL_VALUE + "'")
public class SecurityFilterConfig {

	@Bean
	FilterRegistrationBean<Filter> authenticationPolicyFilterRegistration(final IAuthenticationPolicyFilter authenticationPolicyFilter) {
		final FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>((Filter) authenticationPolicyFilter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	OrchestrationCallbackFilter orchestrationCallbackFilter(final IAuthenticationPolicyFilter authenticationPolicyFilter) {
		return new OrchestrationCallbackFilter(authenticationPolicyFilter);
	}

	@Bean
	FilterRegistrationBean<ArrowheadFilter> orchestrationCallbackFilterRegistration(final OrchestrationCallbackFilter filter) {
		final FilterRegistrationBean<ArrowheadFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setOrder(Constants.REQUEST_FILTER_ORDER_AUTHENTICATION);
		return registration;
	}
}
