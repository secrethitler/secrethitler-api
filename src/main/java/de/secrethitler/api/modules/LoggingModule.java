package de.secrethitler.api.modules;

import de.secrethitler.api.entities.ApplicationLog;
import de.secrethitler.api.services.ApplicationLogService;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Collin Alpert
 */
@Component
public class LoggingModule {

	private static ApplicationLogService service;

	static {
		service = new ApplicationLogService();
	}

	/**
	 * Logs a message in the database.
	 *
	 * @param message The message to log.
	 */
	public void log(String message) {
		try {
			service.create(new ApplicationLog(message));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Logs an exception message with its stack trace to the database.
	 *
	 * @param exception The exception to log.
	 */
	public void log(Throwable exception) {
		var exceptionMessage = Arrays.stream(exception.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n"));

		try {
			service.create(new ApplicationLog(exception.getMessage() + "\n" + exceptionMessage));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
