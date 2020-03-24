package de.secrethitler.api.modules;

import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * @author Collin Alpert
 */
@Component
public class HeaderModule {

	private static final int startIndex = "Basic".length();

	public String getAuthorizationToken(String authorizationHeader) {
		var token = authorizationHeader.substring(startIndex).trim();

		return new String(Base64.getDecoder().decode(token));
	}
}
