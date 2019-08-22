package de.secrethitler.api.modules;

import de.secrethitler.api.entities.ApplicationLog;
import de.secrethitler.api.services.ApplicationLogService;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Collin Alpert
 */
public class LoggingModule {

	private static ApplicationLogService service;

	static {
		service = new ApplicationLogService();
	}

	public void log(String message) {
		try {
			service.create(new ApplicationLog(message));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void log(Throwable exception) {
		var exceptionMessage = Arrays.stream(exception.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n"));

		try {
			service.create(new ApplicationLog(exception.getMessage() + "\n" + exceptionMessage));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
