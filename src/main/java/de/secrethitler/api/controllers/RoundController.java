package de.secrethitler.api.controllers;

import com.github.collinalpert.java2db.database.DBConnection;
import com.github.collinalpert.java2db.queries.OrderTypes;
import de.secrethitler.api.entities.LinkedUserGameRole;
import de.secrethitler.api.entities.Round;
import de.secrethitler.api.exceptions.EmptyOptionalException;
import de.secrethitler.api.modules.EligibilityModule;
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
import java.util.stream.Collectors;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/round")
@CrossOrigin(origins = {"http://10.14.208.75", "http://localhost", "http://localhost:8080", "https://secret-hitler.netlify.com", "https://geheimerdeutscher.tk"}, allowCredentials = "true")
public class RoundController {

	private final GameService gameService;
	private final PusherModule pusherModule;
	private final LinkedUserGameRoleService linkedUserGameRoleService;
	private final RoundService roundService;
	private final EligibilityModule eligibilityModule;

	public RoundController(GameService gameService, PusherModule pusherModule, LinkedUserGameRoleService linkedUserGameRoleService, RoundService roundService, EligibilityModule eligibilityModule) {
		this.gameService = gameService;
		this.pusherModule = pusherModule;
		this.linkedUserGameRoleService = linkedUserGameRoleService;
		this.roundService = roundService;
		this.eligibilityModule = eligibilityModule;
	}

	@PostMapping(value = "/next", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> nextRound(@RequestBody Map<String, Object> requestBody) throws SQLException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		var channelName = (String) requestBody.get("channelName");
		long gameId = this.gameService.getIdByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException("No game was found for the given channelName."));

		var currentRoundOptional = this.roundService.getCurrentRound(gameId);
		var players = this.linkedUserGameRoleService.getMultiple(x -> x.getGameId() == gameId).orderBy(LinkedUserGameRole::getSequenceNumber).toList();

		long nextPresidentId;
		try (var connection = new DBConnection(); var set = connection.execute("select GetNextPresidentId(?);", gameId)) {
			if (set.next()) {
				nextPresidentId = set.getLong(1);
			} else {
				throw new EmptyOptionalException("Next president id could not be calculated");
			}
		}

		var pusher = this.pusherModule.getPusherInstance();
		pusher.trigger(channelName, "next_round", Collections.singletonMap("president_id", nextPresidentId));

		var electableChancellors = players.stream().filter(x -> x.getId() != nextPresidentId);
		if (currentRoundOptional.isPresent()) {
			electableChancellors = electableChancellors.filter(x -> x.getId() != currentRoundOptional.get().getPresidentId() && (currentRoundOptional.get().getChancellorId() == null || x.getId() != currentRoundOptional.get().getChancellorId()));
		}

		var electableChancellorIds = electableChancellors.map(LinkedUserGameRole::getId).collect(Collectors.toList());
		pusher.trigger(String.format("private-%d", nextPresidentId), "notify_president", Collections.singletonMap("electable", electableChancellorIds));

		var newRoundSequenceNumber = currentRoundOptional.map(Round::getSequenceNumber).orElse(0) + 1;
		this.roundService.create(new Round(newRoundSequenceNumber, gameId, nextPresidentId));

		return ResponseEntity.ok(Collections.singletonMap("president_id", nextPresidentId));
	}

	@PostMapping(value = "/special-election", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> specialElection(@RequestBody Map<String, Object> requestBody) throws SQLException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		if (!requestBody.containsKey("nextPresidentId")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "nextPresidentId is missing."));
		}

		var channelName = (String) requestBody.get("channelName");
		var nextPresidentId = (long) requestBody.get("nextPresidentId");

		if (!this.linkedUserGameRoleService.any(x -> x.getId() == nextPresidentId)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "No user was found to be the next president."));
		}

		var game = this.gameService.getByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException(String.format("No game was found for channelName '%s'.", channelName)));
		var gameId = game.getId();

		if (!this.eligibilityModule.isSpecialElectionEligible(game)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Special election is not available yet."));
		}

		var currentRoundSequenceNumber = this.roundService.getMultiple(x -> x.getGameId() == gameId).orderBy(OrderTypes.DESCENDING, Round::getSequenceNumber).limit(1).project(Round::getSequenceNumber).first().orElseThrow(() -> new EmptyOptionalException("No round was found in the current game."));
		this.roundService.create(new Round(currentRoundSequenceNumber + 1, gameId, nextPresidentId, true));

		return ResponseEntity.ok(Collections.emptyMap());
	}
}
