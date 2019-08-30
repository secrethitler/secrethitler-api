package de.secrethitler.api.controllers;

import de.secrethitler.api.entities.Game;
import de.secrethitler.api.entities.LinkedUserGameRole;
import de.secrethitler.api.entities.Round;
import de.secrethitler.api.modules.LoggingModule;
import de.secrethitler.api.modules.PusherModule;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.LinkedUserGameRoleService;
import de.secrethitler.api.services.RoundService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/round")
@CrossOrigin(origins = {"http://localhost:8080", "https://secret-hitler.netlify.com"}, allowCredentials = "true")
public class RoundController {

	private final GameService gameService;
	private final PusherModule pusherModule;
	private final LinkedUserGameRoleService linkedUserGameRoleService;
	private final RoundService roundService;
	private final LoggingModule logger;

	public RoundController(GameService gameService, PusherModule pusherModule, LinkedUserGameRoleService linkedUserGameRoleService, RoundService roundService, LoggingModule logger) {
		this.gameService = gameService;
		this.pusherModule = pusherModule;
		this.linkedUserGameRoleService = linkedUserGameRoleService;
		this.roundService = roundService;
		this.logger = logger;
	}

	@PostMapping(value = "/next", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> nextRound(@RequestBody Map<String, Object> requestBody) {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		var channelName = ((String) requestBody.get("channelName"));
		var gameIdOptional = this.gameService.getSingle(x -> x.getChannelName() == channelName).project(Game::getId).first();
		if (gameIdOptional.isEmpty()) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "No game was found for the given channelName."));
		}

		long gameId = gameIdOptional.get();

		var currentRound = this.roundService.max(Round::getSequenceNumber, x -> x.getGameId() == gameId);
		var players = this.linkedUserGameRoleService.getMultiple(x -> x.getGameId() == gameId).orderBy(LinkedUserGameRole::getSequenceNumber).toList();

		// Modulo will give us the index of the next player in line for the presidency. If no round exists yet, use "0" e.g. "before the first round".
		var playerIndex = Objects.requireNonNullElse(currentRound, 0) % players.size();
		var nextPresidentId = players.get(playerIndex).getUserId();

		var pusher = this.pusherModule.getPusherInstance();
		pusher.trigger(channelName, "next_round", Collections.singletonMap("president_id", nextPresidentId));
		pusher.trigger(String.format("private-%d", nextPresidentId), "notify_president", Collections.emptyMap());

		var newRoundSequenceNumber = Objects.requireNonNullElse(currentRound, 0) + 1;
		var round = new Round(newRoundSequenceNumber, gameId, nextPresidentId);
		try {
			this.roundService.create(round);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.log(e);
			return ResponseEntity.unprocessableEntity().body(Collections.singletonMap("message", e.getMessage()));
		}

		return ResponseEntity.ok(Collections.singletonMap("president_id", nextPresidentId));
	}
}
