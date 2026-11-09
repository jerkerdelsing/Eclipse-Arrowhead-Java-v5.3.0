package eu.arrowhead.localcloudmanager.init;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.init.ApplicationInitListener;
import eu.arrowhead.localcloudmanager.service.RegistrationState;

// Registration in the ServiceRegistry (system LocalCloudManager, localCloudManagement and generalManagement with CERT_AUTH
// interfaces; sysMonitor by the common publisher) is done by the common listener; its result is reported by /health.
@Component
public class LocalCloudManagerApplicationInitListener extends ApplicationInitListener {

	@Autowired
	private RegistrationState registration;

	@Override
	protected void customInit(final ContextRefreshedEvent event) {
		if (standaloneMode) {
			registration.failed("standalone mode: not registered");
			return;
		}

		registration.registered(new ArrayList<>(registeredServices));
	}
}
