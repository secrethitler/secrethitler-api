package de.secrethitler.api;

import de.secrethitler.api.config.DatabaseConfiguration;
import de.secrethitler.api.config.PusherConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		DatabaseConfiguration.class,
		PusherConfiguration.class
})
public class SecretHitlerApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecretHitlerApiApplication.class, args);
	}
}
