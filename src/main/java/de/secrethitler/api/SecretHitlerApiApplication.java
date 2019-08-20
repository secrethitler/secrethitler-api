package de.secrethitler.api;

import com.github.collinalpert.java2db.database.DBConnection;
import de.secrethitler.api.config.DatabaseConfiguration;
import de.secrethitler.api.config.PusherConfiguration;
import de.secrethitler.api.modules.ChannelNameModule;
import de.secrethitler.api.modules.LoggingModule;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.LinkedUserGameRoleService;
import de.secrethitler.api.services.PolicyService;
import de.secrethitler.api.services.RoleService;
import de.secrethitler.api.services.RoundService;
import de.secrethitler.api.services.UserService;
import de.secrethitler.api.services.VoteService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties({
		DatabaseConfiguration.class,
		PusherConfiguration.class
})
public class SecretHitlerApiApplication {

	public static void main(String[] args) {
		var context = SpringApplication.run(SecretHitlerApiApplication.class, args);
		var databaseConfig = context.getBean(DatabaseConfiguration.class);

		DBConnection.HOST = databaseConfig.getHost();
		DBConnection.DATABASE = databaseConfig.getDatabaseName();
		DBConnection.USERNAME = databaseConfig.getUsername();
		DBConnection.PASSWORD = databaseConfig.getPassword();
	}

	@Bean
	GameService getGameService() {
		return new GameService();
	}

	@Bean
	LinkedUserGameRoleService getLinkedUserGameRoleService() {
		return new LinkedUserGameRoleService();
	}

	@Bean
	PolicyService getPolicyService() {
		return new PolicyService();
	}

	@Bean
	RoleService getRoleService() {
		return new RoleService();
	}

	@Bean
	RoundService getRoundService() {
		return new RoundService();
	}

	@Bean
	UserService getUserService() {
		return new UserService();
	}

	@Bean
	VoteService getVoteService() {
		return new VoteService();
	}

	@Bean
	ChannelNameModule getChannelNameModule() {
		return new ChannelNameModule();
	}

	@Bean
	LoggingModule getLoggingModule() {
		return new LoggingModule();
	}
}
