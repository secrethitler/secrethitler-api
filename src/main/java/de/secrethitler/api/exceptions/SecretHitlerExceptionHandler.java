package de.secrethitler.api.exceptions;

import de.secrethitler.api.modules.LoggingModule;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

/**
 * @author Collin Alpert
 */
@ControllerAdvice
public class SecretHitlerExceptionHandler {

	private final LoggingModule logger;

	public SecretHitlerExceptionHandler(LoggingModule loggingModule) {
		this.logger = loggingModule;
	}

	@ExceptionHandler(SQLException.class)
	public ResponseEntity<Map<String, Object>> handleSQLException(SQLException exception) {
		exception.printStackTrace();
		this.logger.log(exception);
		return ResponseEntity.unprocessableEntity().body(Collections.singletonMap("message", exception.getMessage()));
	}

	@ExceptionHandler(EmptyOptionalException.class)
	public ResponseEntity<Map<String, Object>> handleEmptyOptionalException(EmptyOptionalException exception) {
		return ResponseEntity.badRequest().body(Collections.singletonMap("message", exception.getMessage()));
	}
}
