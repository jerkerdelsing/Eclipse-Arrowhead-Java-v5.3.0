package eu.arrowhead.localcloudconfigurator;

import java.util.Arrays;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.jpa.RefreshableRepositoryImpl;
import eu.arrowhead.localcloudconfigurator.cli.ConfiguratorCli;
import eu.arrowhead.localcloudconfigurator.worker.WorkerApplication;

// Without arguments (or with "serve") the Arrowhead support system starts: localCloudConfiguration on the mutual-TLS port 8492.
// "worker" starts the host worker as a non-web Spring application (loopback 8493, mutual TLS). keygen, register, plan and
// executor are the retained operator command-line tools (executor: the legacy ColonyOS executor).
@SpringBootApplication
@ComponentScan(basePackages = Constants.BASE_PACKAGE,
		excludeFilters = {
				// the standard Spring Boot filter: test configurations of other tests stay out of a test's context
				@ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
				@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = eu.arrowhead.common.http.filter.authorization.BlacklistFilter.class),
				// the worker is a separate, non-web application
				@ComponentScan.Filter(type = FilterType.REGEX, pattern = "eu\\.arrowhead\\.localcloudconfigurator\\.worker\\..*")
		})
@EntityScan(LocalCloudConfiguratorConstants.DATABASE_ENTITY_PACKAGE)
@EnableJpaRepositories(basePackages = LocalCloudConfiguratorConstants.DATABASE_REPOSITORY_PACKAGE, repositoryBaseClass = RefreshableRepositoryImpl.class)
public class LocalCloudConfiguratorMain {

	private static final Set<String> TOOLS = Set.of("keygen", "register", "plan", "executor");

	public static void main(final String[] args) throws Exception {
		final String command = args.length == 0 ? "serve" : args[0];
		if (TOOLS.contains(command)) {
			ConfiguratorCli.run(args);
			return;
		}

		if ("worker".equals(command)) {
			// console logging only: the worker has no database for the JDBC log appender
			final String[] workerArgs = Arrays.copyOf(Arrays.copyOfRange(args, 1, args.length), args.length);
			workerArgs[args.length - 1] = "--logging.config=classpath:log4j2-worker.xml";
			new SpringApplicationBuilder(WorkerApplication.class).web(WebApplicationType.NONE).run(workerArgs);
			return;
		}

		final String[] springArgs = "serve".equals(command) && args.length > 0 ? Arrays.copyOfRange(args, 1, args.length) : args;
		SpringApplication.run(LocalCloudConfiguratorMain.class, springArgs);
	}

	protected LocalCloudConfiguratorMain() {
	}
}
