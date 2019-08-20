package de.secrethitler.api.controllers;

import com.pusher.rest.data.PresenceUser;
import de.secrethitler.api.modules.PusherModule;
import de.secrethitler.api.services.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Map;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/pusher")
public class PusherController {

	private final String socketIdParameter = "socket_id";
	private final String channelNameParameter = "channel_name";
	private final String userIdParameter = "user_id";
	private final String userNameParameter = "user_name";

	private final PusherModule pusherModule;
	private final GameService gameService;

	public PusherController(PusherModule pusherModule, GameService gameService) {
		this.pusherModule = pusherModule;
		this.gameService = gameService;
	}

	@CrossOrigin(origins = "*")
	@PostMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<String> authenticatePresence(@RequestParam Map<String, Object> requestBody, HttpSession session) {
		var pusher = this.pusherModule.getPusherInstance();
		var socketId = (String) requestBody.get(this.socketIdParameter);
		var channelName = (String) requestBody.get(this.channelNameParameter);

		if (channelName.startsWith("presence")) {
			var userId = (long) session.getAttribute(this.userIdParameter);
			var userName = (String) session.getAttribute(this.userNameParameter);

			return ResponseEntity.ok(pusher.authenticate(socketId, channelName, new PresenceUser(userId, Collections.singletonMap(this.userNameParameter, userName))));
		} else if (channelName.startsWith("private")) {
			var userId = Long.parseLong(((String) requestBody.get(this.userIdParameter)).split("-")[1]);
			if (userId != ((long) session.getAttribute(this.userIdParameter))) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
			}

			return ResponseEntity.ok(pusher.authenticate(socketId, channelName));
		}

		return ResponseEntity.badRequest().build();
	}
}