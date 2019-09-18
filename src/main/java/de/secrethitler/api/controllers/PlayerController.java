package de.secrethitler.api.controllers;

import de.secrethitler.api.entities.LinkedUserGameRole;
import de.secrethitler.api.enums.RoleTypes;
import de.secrethitler.api.exceptions.EmptyOptionalException;
import de.secrethitler.api.modules.EligibilityModule;
import de.secrethitler.api.modules.PusherModule;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.LinkedUserGameRoleService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/player")
@CrossOrigin(origins = {"http://10.14.208.75", "http://localhost", "http://localhost:8080", "https://secret-hitler.netlify.com", "https://geheimerdeutscher.tk"}, allowCredentials = "true")
public class PlayerController {

	private final GameService gameService;
	private final EligibilityModule eligibilityModule;
	private final LinkedUserGameRoleService linkedUserGameRoleService;
	private final PusherModule pusherModule;

	public PlayerController(GameService gameService, EligibilityModule eligibilityModule, LinkedUserGameRoleService linkedUserGameRoleService, PusherModule pusherModule) {
		this.gameService = gameService;
		this.eligibilityModule = eligibilityModule;
		this.linkedUserGameRoleService = linkedUserGameRoleService;
		this.pusherModule = pusherModule;
	}

	@PostMapping(value = "/execute", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> assassinatePlayer(@RequestBody Map<String, Object> requestBody) throws SQLException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		if (!requestBody.containsKey("userId")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "userId is missing."));
		}

		var channelName = (String) requestBody.get("channelName");
		var userId = (long) requestBody.get("userId");

		long gameId = this.gameService.getIdByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException(String.format("No game was found for the channelName '%s'.", channelName)));

		if (!this.eligibilityModule.isPlayerExecutionEligible(gameId)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Player execution is not available in the current game."));
		}

		var isHitler = this.linkedUserGameRoleService.getSingle(x -> x.getId() == userId).project(LinkedUserGameRole::getRoleId).first().orElseThrow(() -> new EmptyOptionalException("User does not exist in the current game.")) == RoleTypes.SECRET_HITLER.getId();
		if (isHitler) {
			this.pusherModule.trigger(channelName, "game_won", Collections.singletonMap("party", RoleTypes.LIBERAL.getName()));
		}

		this.linkedUserGameRoleService.delete(x -> x.getGameId() == gameId && x.getId() == userId);

		return ResponseEntity.ok(Collections.emptyMap());
	}

	@GetMapping(value = "/investigate/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> investigateLoyalty(@PathVariable("userId") long userId, @RequestParam("channelName") String channelName) {
		if (channelName == null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		var game = this.gameService.getByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException(String.format("No game was found for the channelName '%s'.", channelName)));
		if (!this.eligibilityModule.isLoyaltyInvestigationEligible(game)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Loyalty investigation is not available in the current game."));
		}

		var userRoleId = this.linkedUserGameRoleService.getSingle(x -> x.getId() == userId).project(LinkedUserGameRole::getRoleId).first().orElseThrow(() -> new EmptyOptionalException("Player was not found in the current game."));
		if (userRoleId == RoleTypes.FASCIST.getId() || userId == RoleTypes.SECRET_HITLER.getId()) {
			return ResponseEntity.ok(Collections.singletonMap("message", RoleTypes.FASCIST.getName()));
		}

		return ResponseEntity.ok(Collections.singletonMap("message", RoleTypes.LIBERAL.getName()));
	}
}
