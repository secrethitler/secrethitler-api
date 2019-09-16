package de.secrethitler.api.controllers;

import de.secrethitler.api.entities.LinkedUserGameRole;
import de.secrethitler.api.entities.Round;
import de.secrethitler.api.exceptions.EmptyOptionalException;
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
@CrossOrigin(origins = {"http://10.14.221.66", "http://localhost", "http://localhost:8080", "https://secret-hitler.netlify.com", "https://geheimerdeutscher.tk"}, allowCredentials = "true")
public class RoundController {

	private final GameService gameService;
	private final PusherModule pusherModule;
	private final LinkedUserGameRoleService linkedUserGameRoleService;
	private final RoundService roundService;

	public RoundController(GameService gameService, PusherModule pusherModule, LinkedUserGameRoleService linkedUserGameRoleService, RoundService roundService) {
		this.gameService = gameService;
		this.pusherModule = pusherModule;
		this.linkedUserGameRoleService = linkedUserGameRoleService;
		this.roundService = roundService;
	}

	@PostMapping(value = "/next", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> nextRound(@RequestBody Map<String, Object> requestBody) throws SQLException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		var channelName = ((String) requestBody.get("channelName"));
		long gameId = this.gameService.getIdByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException("No game was found for the given channelName."));

		var currentRoundOptional = this.roundService.getCurrentRound(gameId);
		var players = this.linkedUserGameRoleService.getMultiple(x -> x.getGameId() == gameId).orderBy(LinkedUserGameRole::getSequenceNumber).toList();

		// Modulo will give us the index of the next player in line for the presidency. If no round exists yet, use "0" e.g. "before the first round".
		var playerIndex = currentRoundOptional.map(Round::getSequenceNumber).orElse(0) % players.size();
		var nextPresidentId = players.get(playerIndex).getUserId();

		var pusher = this.pusherModule.getPusherInstance();
		pusher.trigger(channelName, "next_round", Collections.singletonMap("president_id", nextPresidentId));

		var electableChancellors = players.stream().filter(x -> x.getUserId() != nextPresidentId);
		if (currentRoundOptional.isPresent()) {
			electableChancellors = electableChancellors.filter(x -> x.getUserId() != currentRoundOptional.get().getPresidentId() && (currentRoundOptional.get().getChancellorId() == null || x.getUserId() != currentRoundOptional.get().getChancellorId()));
		}

		var electableChancellorIds = electableChancellors.map(LinkedUserGameRole::getUserId).collect(Collectors.toList());
		pusher.trigger(String.format("private-%d", nextPresidentId), "notify_president", Collections.singletonMap("electable", electableChancellorIds));

		var newRoundSequenceNumber = currentRoundOptional.map(Round::getSequenceNumber).orElse(0) + 1;
		this.roundService.create(new Round(newRoundSequenceNumber, gameId, nextPresidentId));

		return ResponseEntity.ok(Collections.singletonMap("president_id", nextPresidentId));
	}
}
