package de.secrethitler.api.controllers;

import com.github.collinalpert.java2db.queries.OrderTypes;
import de.secrethitler.api.entities.Game;
import de.secrethitler.api.entities.Round;
import de.secrethitler.api.modules.LoggingModule;
import de.secrethitler.api.services.GameService;
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

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/chancellor")
@CrossOrigin(origins = {"http://localhost:8080", "https://secret-hitler.netlify.com"}, allowCredentials = "true")
public class ChancellorController {

	private final GameService gameService;
	private final RoundService roundService;
	private final LoggingModule logger;

	public ChancellorController(GameService gameService, RoundService roundService, LoggingModule logger) {
		this.gameService = gameService;
		this.roundService = roundService;
		this.logger = logger;
	}

	@PostMapping(value = "/nominate", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> nominateChancellor(@RequestBody Map<String, Object> requestBody) {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		if (!requestBody.containsKey("chancellorId")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "chancellorId is missing."));
		}

		var channelName = (String) requestBody.get("channelName");
		var chancellorId = (int) requestBody.get("chancellorId");

		var gameIdOptional = this.gameService.getSingle(x -> x.getChannelName() == channelName).project(Game::getId).first();
		if (gameIdOptional.isEmpty()) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "No game was found for the given channelName."));
		}

		long gameId = gameIdOptional.get();
		var previousRoundOptional = this.roundService.getMultiple(x -> x.getGameId() == gameId).orderBy(OrderTypes.DESCENDING, Round::getId).limit(1).first();
		if (previousRoundOptional.isEmpty()) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "No round was found for the given channelName"));
		}

		var previousRound = previousRoundOptional.get();
		if (previousRound.getChancellorId() == chancellorId || previousRound.getPresidentId() == chancellorId) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Nominated chancellor was either president or chancellor in the previous round."));
		}

		try {
			this.roundService.update(previousRound.getId(), Round::getNominatedChancellorId, chancellorId);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.log(e);
			return ResponseEntity.unprocessableEntity().body(Collections.singletonMap("message", e.getMessage()));
		}

		return ResponseEntity.ok(Collections.emptyMap());
	}
}
