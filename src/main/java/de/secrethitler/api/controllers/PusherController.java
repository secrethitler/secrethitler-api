package de.secrethitler.api.controllers;

import com.pusher.rest.data.PresenceUser;
import de.secrethitler.api.modules.PusherModule;
import de.secrethitler.api.services.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Objects;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/pusher")
@CrossOrigin(origins = {"http://localhost:8080", "https://secret-hitler.netlify.com"}, allowCredentials = "true")
public class PusherController {

	private final PusherModule pusherModule;
	private final GameService gameService;

	public PusherController(PusherModule pusherModule, GameService gameService) {
		this.pusherModule = pusherModule;
		this.gameService = gameService;
	}

	@PostMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> authenticateJson(@RequestBody Map<String, Object> request, HttpSession session) {
		if (!request.containsKey("socketId") || !request.containsKey("channelName")) {
			return ResponseEntity.badRequest().body("Parameters are missing");
		}

		return authenticate((String) request.get("socketId"), (String) request.get("channelName"), session);
	}

	@PostMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<String> authenticateForm(@RequestParam("socket_id") String socketId, @RequestParam("channel_name") String channelName, HttpSession session) {
		return authenticate(socketId, channelName, session);
	}

	private ResponseEntity<String> authenticate(String socketId, String channelName, HttpSession session) {
		var pusher = this.pusherModule.getPusherInstance();

		if (channelName.startsWith("presence")) {
			var userId = (long) Objects.requireNonNullElse(session.getAttribute("userId"), 1L);
			var userName = (String) Objects.requireNonNullElse(session.getAttribute("userName"), "Vladimir");

			boolean isChannelCreator = this.gameService.getCreatorIdByChannelName(channelName.split("-")[1]) == userId;
			var responseData = Map.of("user_name", userName, "is_channel_creator", isChannelCreator);

			return ResponseEntity.ok(pusher.authenticate(socketId, channelName, new PresenceUser(userId, responseData)));
		} else if (channelName.startsWith("private")) {
			var userIdText = channelName.split("-")[1];
			long userId;

			try {
				userId = Long.parseLong(userIdText);
			} catch (NumberFormatException e) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\": \"The value " + userIdText + " is not a valid userId, Du Horst.\"}");
			}

			if (userId != ((long) Objects.requireNonNullElse(session.getAttribute("userId"), 1L))) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\": \"The passed userId does not match the one in the session.\"}");
			}

			return ResponseEntity.ok(pusher.authenticate(socketId, channelName));
		}

		return ResponseEntity.badRequest().build();
	}
}