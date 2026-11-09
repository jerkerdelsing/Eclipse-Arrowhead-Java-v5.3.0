package eu.arrowhead.localcloudmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import eu.arrowhead.common.Constants;

// LocalCloudManager: the operator console (loopback connector 8490, password login) and the Arrowhead consumer system
// LocalCloudManager (mutual-TLS connector 8491). No database: it keeps no persistent state.
@SpringBootApplication
@ComponentScan(basePackages = Constants.BASE_PACKAGE,
		excludeFilters = {
				@ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
				@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = eu.arrowhead.common.http.filter.authorization.BlacklistFilter.class),
				// replaced by ConsoleSecurityConfig: the console connector stays on HTTP with its own login
				@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = eu.arrowhead.common.security.DefaultSecurityConfig.class)
		})
public class LocalCloudManagerMain {

	public static void main(final String[] args) {
		SpringApplication.run(LocalCloudManagerMain.class, args);
	}

	protected LocalCloudManagerMain() {
	}
}
