/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.security;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.filter.authentication.IAuthenticationPolicyFilter;
import eu.arrowhead.onboarding.OnboardingConstants;
/**
 * The device steps are verified by the onboarding controller (bootstrap, on and de certificates, which have no identity
 * token); general management and onboardingManagement use the normal cloud identity filter.
 */
@Configuration
public class OnboardingSecurityConfig {
    @Bean
    FilterRegistrationBean<IAuthenticationPolicyFilter> cloudAuthenticationRegistration(IAuthenticationPolicyFilter filter) {
        FilterRegistrationBean<IAuthenticationPolicyFilter> bean = new FilterRegistrationBean<>(filter);
        bean.addUrlPatterns("/onboarding/general/*", OnboardingConstants.HTTP_API_MANAGEMENT_PATH + "/*");
        bean.setOrder(Constants.REQUEST_FILTER_ORDER_AUTHENTICATION);
        return bean;
    }
}
