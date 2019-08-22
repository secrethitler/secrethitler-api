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
import java.util.Map;
import java.util.Objects;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/pusher")
public class PusherController {

	private final PusherModule pusherModule;
	private final GameService gameService;

	public PusherController(PusherModule pusherModule, GameService gameService) {
		this.pusherModule = pusherModule;
		this.gameService = gameService;
	}

	@CrossOrigin
	@PostMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<String> authenticatePresence(@RequestParam("socket_id") String socketId, @RequestParam("channel_name") String channelName, HttpSession session) {
		var pusher = this.pusherModule.getPusherInstance();

		if (channelName.startsWith("presence")) {
			var userId = (long) Objects.requireNonNullElse(session.getAttribute("userId"), 1L);
			var userName = (String) Objects.requireNonNullElse(session.getAttribute("userName"), "Vladimir");

			boolean isChannelCreator = this.gameService.getCreatorIdByChannelName(channelName.split("-")[1]) == userId;
			var responseData = Map.of("user_name", userName, "is_channel_creator", isChannelCreator);

			return ResponseEntity.ok(pusher.authenticate(socketId, channelName, new PresenceUser(userId, responseData)));
		} else if (channelName.startsWith("private")) {
			var userId = Long.parseLong(channelName.split("-")[1]);
			if (userId != ((long) Objects.requireNonNullElse(session.getAttribute("userId"), 1L))) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
			}

			return ResponseEntity.ok(pusher.authenticate(socketId, channelName));
		}

		return ResponseEntity.badRequest().build();
	}
}