package de.secrethitler.api.controllers;

import de.secrethitler.api.entities.Game;
import de.secrethitler.api.entities.User;
import de.secrethitler.api.modules.ChannelNameModule;
import de.secrethitler.api.modules.LoggingModule;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/game")
public class GameController {

	private final String userNameParameter = "user_name";
	private final String userIdParameter = "user_id";
	private final String channelNameParameter = "channel_name";
	private final String creatorIdParameter = "creator_id";

	private final UserService userService;
	private final ChannelNameModule channelNameModule;
	private final GameService gameService;
	private final LoggingModule logger;

	public GameController(UserService userService, ChannelNameModule channelNameModule, GameService gameService, LoggingModule logger) {
		this.userService = userService;
		this.channelNameModule = channelNameModule;
		this.gameService = gameService;
		this.logger = logger;
	}

	@CrossOrigin(origins = "*")
	@PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> createGame(@RequestBody Map<String, Object> requestBody, HttpSession session) {
		var userName = (String) requestBody.get(this.userNameParameter);
		var channelName = this.channelNameModule.generateChannelName();

		// Keep generating new channel names until a unique one is found.
		while (channelNameAlreadyExists(channelName)) {
			channelName = this.channelNameModule.generateChannelName();
		}

		long userId;
		try {
			var user = new User(userName);
			userId = this.userService.create(user);
			var game = new Game(userId, channelName);
			this.gameService.create(game);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.log(e);
			return ResponseEntity.badRequest().build();
		}

		// Add to session
		session.setAttribute(this.userNameParameter, userName);
		session.setAttribute(this.userIdParameter, userId);
		session.setAttribute(this.channelNameParameter, channelName);

		return ResponseEntity.ok(Map.of(this.userNameParameter, userName, this.userIdParameter, userId, this.channelNameParameter, channelName));

	}

	@CrossOrigin(origins = "*")
	@PostMapping(value = "/join", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> joinGame(@RequestBody Map<String, Object> requestBody, HttpSession session) {

		var userName = (String) requestBody.get(this.userNameParameter);
		var channelName = (String) requestBody.get(this.channelNameParameter);

		if (!gameService.any(x -> x.getChannelname() == channelName)) {
			return ResponseEntity.unprocessableEntity().build();
		}

		var user = new User(userName);

		long userId;
		try {
			userId = this.userService.create(user);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.log(e);
			return ResponseEntity.badRequest().build();
		}

		var creatorId = this.gameService.getCreatorIdByChannelName(channelName);

		return ResponseEntity.ok(Map.of(this.userIdParameter, userId, this.userNameParameter, userName, this.channelNameParameter, channelName, this.creatorIdParameter, creatorId));


	}

	private boolean channelNameAlreadyExists(String channelName) {
		return this.gameService.any(x -> x.getChannelname() == channelName);
	}
}
