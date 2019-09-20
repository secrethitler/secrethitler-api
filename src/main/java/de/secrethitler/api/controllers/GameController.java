package de.secrethitler.api.controllers;

import com.google.gson.JsonParser;
import de.secrethitler.api.entities.Game;
import de.secrethitler.api.entities.LinkedUserGameRole;
import de.secrethitler.api.entities.model.PlayerRole;
import de.secrethitler.api.enums.RoleTypes;
import de.secrethitler.api.exceptions.EmptyOptionalException;
import de.secrethitler.api.modules.ChannelNameModule;
import de.secrethitler.api.modules.PusherModule;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.LinkedUserGameRoleService;
import de.secrethitler.api.util.PlayerRoleDistribution;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/game")
@CrossOrigin(allowCredentials = "true")
public class GameController {

	private final ChannelNameModule channelNameModule;
	private final GameService gameService;
	private final PusherModule pusherModule;
	private final LinkedUserGameRoleService linkedUserGameRoleService;

	public GameController(ChannelNameModule channelNameModule, GameService gameService, PusherModule pusherModule, LinkedUserGameRoleService linkedUserGameRoleService) {
		this.channelNameModule = channelNameModule;
		this.gameService = gameService;
		this.pusherModule = pusherModule;
		this.linkedUserGameRoleService = linkedUserGameRoleService;
	}

	@PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> createGame(@RequestBody Map<String, Object> requestBody, HttpSession session) throws SQLException {
		if (!requestBody.containsKey("userName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "userName is missing."));
		}

		var userName = (String) requestBody.get("userName");
		var channelName = this.channelNameModule.generateChannelName();

		// Keep generating new channel name until a unique one is found.
		while (channelNameAlreadyExists(channelName)) {
			channelName = this.channelNameModule.generateChannelName();
		}

		// Create seed for card shuffling between 1 and 999.
		var cardStackSeed = (int) (Math.random() * (999 - 1) + 1);
		var game = new Game(channelName, 11, 6, cardStackSeed);
		var gameId = this.gameService.create(game);

		var userId = this.linkedUserGameRoleService.create(new LinkedUserGameRole(userName, gameId));
		this.gameService.update(gameId, Game::getCreatorId, userId);

		// Add to session
		session.setAttribute("userName", userName);
		session.setAttribute("userId", userId);
		session.setAttribute("channelName", channelName);

		return ResponseEntity.ok(Map.of("userName", userName, "userId", userId, "channelName", channelName));
	}

	@PostMapping(value = "/join", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> joinGame(@RequestBody Map<String, Object> requestBody, HttpSession session) throws SQLException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		if (!requestBody.containsKey("userName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "userName is missing."));
		}

		var userName = (String) requestBody.get("userName");
		var channelName = (String) requestBody.get("channelName");

		var gameId = this.gameService.getIdByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException(String.format("No game was found for the channelName '%s'.", channelName)));

		if (this.linkedUserGameRoleService.any(x -> x.getGameId() == gameId && x.getUserName() == userName)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("message", "User has already joined."));
		}

		var userId = this.linkedUserGameRoleService.create(new LinkedUserGameRole(userName, gameId));

		session.setAttribute("userId", userId);
		session.setAttribute("userName", userName);
		session.setAttribute("channelName", channelName);

		var creatorId = this.gameService.getCreatorIdByChannelName(channelName);

		return ResponseEntity.ok(Map.of("userId", userId, "userName", userName, "channelName", channelName, "creatorId", creatorId));
	}

	@PostMapping(value = "/start", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> startGame(@RequestBody Map<String, Object> requestBody, HttpSession session) throws SQLException, ExecutionException, InterruptedException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Channel name cannot be null."));
		}

		var sessionUserId = session.getAttribute("userId");
		if (sessionUserId == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "There was no userId found in session."));
		}

		var userId = (long) sessionUserId;
		var channelName = (String) requestBody.get("channelName");

		var game = this.gameService.getByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException(String.format("No game was found for the channelName '%s'.", channelName)));
		var gameId = game.getId();

		// Game is trying to be started by someone other than the creator of the game. This is forbidden.
		if (game.getCreatorId() != userId) {
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

		if (usersInPresenceChannel.size() != this.linkedUserGameRoleService.count(x -> x.getGameId() == gameId)) {
			return ResponseEntity.badRequest().body(Map.of("message", "Users in presence channel do not match the number of players joined."));
		}

		this.gameService.update(gameId, Game::getInitialPlayerCount, usersInPresenceChannel.size());

		var roleDistribution = new PlayerRoleDistribution(usersInPresenceChannel.size());
		var playerRoles = new PlayerRole[usersInPresenceChannel.size()];
		var tasks = new CompletableFuture[usersInPresenceChannel.size()];

		for (int i = 0; i < usersInPresenceChannel.size(); i++) {
			var presenceUserId = usersInPresenceChannel.get(i).getAsJsonObject().get("id").getAsLong();
			var roleType = roleDistribution.getNextRole();
			var userName = this.linkedUserGameRoleService.getSingle(x -> x.getId() == presenceUserId).project(LinkedUserGameRole::getUserName).first().orElseThrow(() -> new EmptyOptionalException(String.format("No user with id %d found", presenceUserId)));

			playerRoles[i] = new PlayerRole(presenceUserId, roleType.getId(), roleType.getName(), userName);
			tasks[i] = this.linkedUserGameRoleService.updateAsync(presenceUserId, LinkedUserGameRole::getRoleId, roleType.getId());
		}

		// Wait for the update tasks to finish.
		CompletableFuture.allOf(tasks).get();

		// Get all fascists.
		var fascists = Arrays.stream(playerRoles).filter(x -> x.getRoleId() == RoleTypes.FASCIST.getId() || x.getRoleId() == RoleTypes.SECRET_HITLER.getId()).map(PlayerRole::new).collect(Collectors.toList());

		for (var player : playerRoles) {
			// If the player is fascist, inform him of his party members. In a game of five and six players, the secret hitler also knows the fascist players.
			if (player.getRoleId() == RoleTypes.FASCIST.getId() || (player.getRoleId() == RoleTypes.SECRET_HITLER.getId()) && usersInPresenceChannel.size() >= 5 && usersInPresenceChannel.size() <= 6) {
				player.setPartyMembers(fascists);
			}

			pusher.trigger("private-" + player.getUserId(), "game_start", player);
		}

		return ResponseEntity.ok(Collections.emptyMap());
	}

	private boolean channelNameAlreadyExists(String channelName) {
		return this.gameService.any(x -> x.getChannelName() == channelName);
	}
}
