package de.secrethitler.api;

import com.github.collinalpert.java2db.database.DBConnection;
import de.secrethitler.api.config.DatabaseConfiguration;
import de.secrethitler.api.config.PusherConfiguration;
import de.secrethitler.api.modules.LoggingModule;
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
	LoggingModule getLoggingModule() {
		return new LoggingModule();
	}
}
