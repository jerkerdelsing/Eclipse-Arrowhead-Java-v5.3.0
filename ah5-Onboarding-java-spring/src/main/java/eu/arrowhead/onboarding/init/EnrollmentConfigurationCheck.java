/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.init;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
/** Fail closed rather than silently starting enrollment with development security defaults. */
@Component
public class EnrollmentConfigurationCheck implements InitializingBean {
    private final Environment env;
    public EnrollmentConfigurationCheck(Environment env) { this.env = env; }
    @Override public void afterPropertiesSet() {
        require("server.ssl.enabled", "true");
        require("server.ssl.client-auth", "need");
        require("authentication.policy", "outsourced");
        require("enable.management.filter", "true");
        require("log.all.request.and.response", "false");
        require("mqtt.api.enabled", "false");
        require("enable.blacklist.filter", "false");
        require("disable.hostname.verifier", "false");
    }
    private void require(String key, String expected) {
        if (!expected.equalsIgnoreCase(env.getProperty(key))) { throw new IllegalStateException(key + " must be " + expected + " for Onboarding"); }
    }
}
