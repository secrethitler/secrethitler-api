package de.secrethitler.api.controllers;

import com.github.collinalpert.java2db.utilities.FunctionUtils;
import com.google.gson.JsonParser;
import de.secrethitler.api.entities.Game;
import de.secrethitler.api.entities.LinkedUserGameRole;
import de.secrethitler.api.entities.Role;
import de.secrethitler.api.entities.User;
import de.secrethitler.api.entities.model.PlayerRole;
import de.secrethitler.api.enums.RoleTypes;
import de.secrethitler.api.modules.ChannelNameModule;
import de.secrethitler.api.modules.LoggingModule;
import de.secrethitler.api.modules.PusherModule;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.LinkedUserGameRoleService;
import de.secrethitler.api.services.RoleService;
import de.secrethitler.api.services.UserService;
import de.secrethitler.api.util.PlayerRoleDistribution;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
	private final PusherModule pusherModule;
	private final RoleService roleService;
	private final LinkedUserGameRoleService linkedUserGameRoleService;

	public GameController(UserService userService, ChannelNameModule channelNameModule, GameService gameService, LoggingModule logger, PusherModule pusherModule, RoleService roleService, LinkedUserGameRoleService linkedUserGameRoleService) {
		this.userService = userService;
		this.channelNameModule = channelNameModule;
		this.gameService = gameService;
		this.logger = logger;
		this.pusherModule = pusherModule;
		this.roleService = roleService;
		this.linkedUserGameRoleService = linkedUserGameRoleService;
	}

	@PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> createGameJson(@RequestBody User request, HttpSession session) {
		return createGame(request, session);
	}

	@PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<Map<String, Object>> createGameForm(@RequestParam("userName") String userName, HttpSession session) {
		return createGame(new User(userName), session);
	}

	@PostMapping(value = "/join", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> joinGameJson(@RequestBody Map<String, Object> requestBody, HttpSession session) {
		var userName = (String) requestBody.get("userName");
		var channelName = (String) requestBody.get("channelName");

		return joinGame(userName, channelName, session);
	}

	@PostMapping(value = "/join", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<Map<String, Object>> joinGameForm(@RequestParam("userName") String userName, @RequestParam("channelName") String channelName, HttpSession session) {
		return joinGame(userName, channelName, session);
	}

	@PostMapping(value = "/start", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> startGameJson(@RequestBody Map<String, Object> requestBody, HttpSession session) {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Channel name cannot be null."));
		}

		return startGame((String) requestBody.get("channelName"), session);
	}

	@PostMapping(value = "/start", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<Map<String, Object>> startGameForm(@RequestParam("channelName") String channelName, HttpSession session) {
		return startGame(channelName, session);
	}

	private ResponseEntity<Map<String, Object>> startGame(String channelName, HttpSession session) {
		var sessionUserId = session.getAttribute("userId");
		if (sessionUserId == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "There was no userId found in session."));
		}

		var userId = (long) sessionUserId;
		var game = this.gameService.getSingle(x -> x.getChannelName() == channelName).first();

		// Game is trying to be started by someone other than the creator of the game. This is forbidden.
		if (game.isEmpty() || game.get().getCreatorId() != userId) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Game cannot be started by someone else than the game creator."));
		}

		// Get all users in the presence channel.
		var pusher = this.pusherModule.getPusherInstance();
		var result = pusher.get(String.format("/channels/%s%s/users", "presence-", channelName));

		var object = new JsonParser().parse(result.getMessage()).getAsJsonObject();
		var usersInPresenceChannel = object.getAsJsonArray("users");

		if (usersInPresenceChannel.size() < 5 || usersInPresenceChannel.size() > 10) {
			return ResponseEntity.unprocessableEntity().body(Map.of("message", "The number of players is not valid."));
		}

		this.gameService.updateAsync(game.get().getId(), Game::getInitialPlayerCount, usersInPresenceChannel.size(), logger::log);

		var roleDistribution = new PlayerRoleDistribution(usersInPresenceChannel.size());
		var playerRoles = new ArrayList<PlayerRole>(usersInPresenceChannel.size());

		var roles = this.roleService.getMultiple(x -> true).toMap(Role::getId, Role::getName);

		var playerSequenceNumber = 0;
		for (var user : usersInPresenceChannel) {
			var presenceUserId = user.getAsJsonObject().get("id").getAsLong();
			var roleType = roleDistribution.getNextRole();
			var userName = this.userService.getSingle(x -> x.getId() == presenceUserId).project(User::getUserName).first().orElse(null);

			playerRoles.add(new PlayerRole(presenceUserId, roleType.getId(), roles.get(roleType.getId()), userName));
			linkedUserGameRoleService.createAsync(new LinkedUserGameRole(presenceUserId, game.get().getId(), roleType.getId(), ++playerSequenceNumber), FunctionUtils.empty(), logger::log);
		}

		var fascists = getFascists(playerRoles);

		for (var player : playerRoles) {
			// If the player is fascist, inform him of his party members. In a game of five and six players, the secret hitler also knows the fascist players.
			if (player.getRoleId() == RoleTypes.FASCIST.getId() || (player.getRoleId() == RoleTypes.SECRET_HITLER.getId() && usersInPresenceChannel.size() >= 5 && usersInPresenceChannel.size() <= 6)) {
				player.setPartyMembers(fascists);
			}

			pusher.trigger("private-" + player.getUserId(), "game_start", player);
		}

		return ResponseEntity.ok(Collections.emptyMap());
	}

	/**
	 * Get all fascists from a list of players.
	 *
	 * @param playerRoles All players.
	 * @return A list of player info of the players who are fascist.
	 */
	private List<PlayerRole> getFascists(List<PlayerRole> playerRoles) {
		return playerRoles.stream().filter(x -> x.getRoleId() == RoleTypes.FASCIST.getId() || x.getRoleId() == RoleTypes.SECRET_HITLER.getId()).map(PlayerRole::new).collect(Collectors.toList());
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
