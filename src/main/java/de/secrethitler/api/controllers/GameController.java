package de.secrethitler.api.controllers;

import de.secrethitler.api.entities.Game;
import de.secrethitler.api.entities.User;
import de.secrethitler.api.modules.ChannelNameModule;
import de.secrethitler.api.modules.LoggingModule;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = {"http://localhost:8080", "https://secret-hitler.netlify.com"}, allowCredentials = "true")
public class GameController {

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


	@PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> createGameJson(@RequestBody User request, HttpSession session) {
		return createGame(request, session);
	}

	@PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<Map<String, Object>> createGameForm(@RequestParam("userName") String userName, HttpSession session) {
		return createGame(new User(userName), session);
	}

	@PostMapping(value = "/join", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<Map<String, Object>> joinGameForm(@RequestParam("userName") String userName, @RequestParam("channelName") String channelName, HttpSession session) {
		return joinGame(userName, channelName, session);
	}

	@PostMapping(value = "/join", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> joinGameForm(@RequestBody Map<String, Object> requestBody, HttpSession session) {
		var userName = (String) requestBody.get("userName");
		var channelName = (String) requestBody.get("channelName");

		return joinGame(userName, channelName, session);
	}

	private ResponseEntity<Map<String, Object>> createGame(User user, HttpSession session) {
		var userName = user.getUserName();
		var channelName = this.channelNameModule.generateChannelName();

		// Keep generating new channel names until a unique one is found.
		while (channelNameAlreadyExists(channelName)) {
			channelName = this.channelNameModule.generateChannelName();
		}

		long userId;
		try {
			userId = this.userService.create(user);
			var game = new Game(userId, channelName);
			this.gameService.create(game);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.log(e);
			return ResponseEntity.badRequest().build();
		}

		// Add to session
		session.setAttribute("userName", userName);
		session.setAttribute("userId", userId);
		session.setAttribute("channelName", channelName);

		return ResponseEntity.ok(Map.of("userName", userName, "userId", userId, "channelName", channelName));
	}

	private ResponseEntity<Map<String, Object>> joinGame(String userName, String channelName, HttpSession session) {
		if (!gameService.any(x -> x.getChannelName() == channelName)) {
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

		session.setAttribute("userId", userId);
		session.setAttribute("userName", userName);
		session.setAttribute("channelName", channelName);

		var creatorId = this.gameService.getCreatorIdByChannelName(channelName);

		return ResponseEntity.ok(Map.of("userId", userId, "userName", userName, "channelName", channelName, "creatorId", creatorId));


	}

	private boolean channelNameAlreadyExists(String channelName) {
		return this.gameService.any(x -> x.getChannelName() == channelName);
	}
}
