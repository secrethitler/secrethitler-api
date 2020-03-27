package de.secrethitler.api.controllers;

import com.pusher.rest.data.PresenceUser;
import de.secrethitler.api.entities.LinkedUserGameRole;
import de.secrethitler.api.exceptions.EmptyOptionalException;
import de.secrethitler.api.modules.NumberModule;
import de.secrethitler.api.modules.PusherModule;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.LinkedUserGameRoleService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/pusher")
@CrossOrigin(allowCredentials = "true")
public class PusherController {

	private final PusherModule pusherModule;
	private final GameService gameService;
	private final NumberModule numberModule;
	private final LinkedUserGameRoleService linkedUserGameRoleService;

	public PusherController(PusherModule pusherModule, GameService gameService, NumberModule numberModule, LinkedUserGameRoleService linkedUserGameRoleService) {
		this.pusherModule = pusherModule;
		this.gameService = gameService;
		this.numberModule = numberModule;
		this.linkedUserGameRoleService = linkedUserGameRoleService;
	}

	/**
	 * Authenticates a client with the Pusher library.
	 *
	 * @param requestBody The request's body, containing the channelName of the game and the socketId the client established a connection with Pusher on.
	 * @return The Pusher authentication response. See the Pusher docs for more information.
	 */
	@PostMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> authenticate(@RequestBody Map<String, Object> requestBody, @RequestHeader(HttpHeaders.AUTHORIZATION) String base64Token) {
		if (!requestBody.containsKey("socketId") || !requestBody.containsKey("channelName") || !requestBody.containsKey("userId")) {
			return ResponseEntity.badRequest().body("Parameters are missing");
		}

		var socketId = (String) requestBody.get("socketId");
		var channelName = (String) requestBody.get("channelName");
		var userId = this.numberModule.getAsLong(requestBody.get("userId"));

		if (!linkedUserGameRoleService.hasValidToken(userId, base64Token)) {
			return ResponseEntity.badRequest().body("{\"message\": \"Invalid authorization.\"}");
		}

		var pusher = this.pusherModule.getPusherInstance();

		if (channelName.startsWith("presence")) {

			var userName = this.linkedUserGameRoleService.getSingle(x -> x.getId() == userId).project(LinkedUserGameRole::getUserName).first().orElseThrow(() -> new EmptyOptionalException("No username found."));
			boolean isChannelCreator = this.gameService.getCreatorIdByChannelName(channelName.split("-")[1]) == userId;
			var responseData = Map.of("userName", userName, "isChannelCreator", isChannelCreator);

			return ResponseEntity.ok(pusher.authenticate(socketId, channelName, new PresenceUser(userId, responseData)));
		} else if (channelName.startsWith("private")) {
			var userIdText = channelName.split("-")[1];
			long parsedUserId;

			try {
				parsedUserId = Long.parseLong(userIdText);
			} catch (NumberFormatException e) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\": \"The value " + userIdText + " is not a valid userId.\"}");
			}

			if (userId != parsedUserId) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\": \"The passed userId does not match the one in the authorization.\"}");
			}

			return ResponseEntity.ok(pusher.authenticate(socketId, channelName));
		}

		return ResponseEntity.badRequest().build();
	}
}