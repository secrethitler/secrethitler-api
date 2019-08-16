package de.secrethitler.api.response;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * @author Collin Alpert
 */
public class ResponseObject {

	private final int statusCode;
	private final String message;
	private String name = null;
	private Object value = null;

	public ResponseObject(String name, Object value, int statusCode, String message) {
		this.name = name;
		this.value = value;
		this.statusCode = statusCode;
		this.message = message;
	}

	public ResponseObject(int statusCode, String message) {
		this.statusCode = statusCode;
		this.message = message;
	}

	public static ResponseEntity<ResponseObject> badRequest(String message) {
		return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(new ResponseObject(400, message));
	}

	public static ResponseEntity<ResponseObject> badRequest(Throwable exception) {
		return badRequest(exception.getMessage());
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getMessage() {
		return message;
	}
}
