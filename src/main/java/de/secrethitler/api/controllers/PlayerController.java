package de.secrethitler.api.controllers;

import de.secrethitler.api.entities.LinkedUserGameRole;
import de.secrethitler.api.enums.RoleTypes;
import de.secrethitler.api.exceptions.EmptyOptionalException;
import de.secrethitler.api.modules.EligibilityModule;
import de.secrethitler.api.modules.NumberModule;
import de.secrethitler.api.modules.PusherModule;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.LinkedUserGameRoleService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
@RequestMapping("/player")
@CrossOrigin(allowCredentials = "true")
public class PlayerController {

	private final GameService gameService;
	private final EligibilityModule eligibilityModule;
	private final LinkedUserGameRoleService linkedUserGameRoleService;
	private final PusherModule pusherModule;
	private final NumberModule numberModule;

	public PlayerController(GameService gameService,
							EligibilityModule eligibilityModule,
							LinkedUserGameRoleService linkedUserGameRoleService,
							PusherModule pusherModule,
							NumberModule numberModule) {
		this.gameService = gameService;
		this.eligibilityModule = eligibilityModule;
		this.linkedUserGameRoleService = linkedUserGameRoleService;
		this.pusherModule = pusherModule;
		this.numberModule = numberModule;
	}

	/**
	 * Executes a player. This is an executive action.
	 * Also contains the game-over handling, if Hitler is executed.
	 *
	 * @param requestBody The request's body, containing the channelName of the game and the userId of the user to execute.
	 * @return A successful 200 HTTP response.
	 * @throws SQLException The exception which can occur when interchanging with the database.
	 */
	@PostMapping(value = "/execute", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> executePlayer(@RequestBody Map<String, Object> requestBody, @RequestHeader(HttpHeaders.AUTHORIZATION) String base64Token) throws SQLException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		if (!requestBody.containsKey("executedUserId")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "executedUserId is missing."));
		}

		if (!requestBody.containsKey("userId")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "userId is missing."));
		}

		var channelName = (String) requestBody.get("channelName");
		var executedUserId = this.numberModule.getAsLong(requestBody.get("executedUserId"));
		var userId = this.numberModule.getAsLong(requestBody.get("userId"));

		if (!this.linkedUserGameRoleService.hasValidToken(userId, base64Token)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Invalid authorization."));
		}

		long gameId = this.gameService.getIdByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException(String.format("No game was found for the channelName '%s'.", channelName)));

		if (!this.eligibilityModule.isPlayerExecutionEligible(gameId)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Player execution is not available in the current game."));
		}

		var isHitler = this.linkedUserGameRoleService.getSingle(x -> x.getId() == executedUserId && !x.isExecuted()).project(LinkedUserGameRole::getRoleId).first().orElseThrow(() -> new EmptyOptionalException("User does not exist in the current game.")) == RoleTypes.SECRET_HITLER.getId();
		if (isHitler) {
			this.pusherModule.trigger(channelName, "gameWon", Map.of("party", RoleTypes.LIBERAL.getName(), "reason", "Hitler was executed!"));
		}

		this.linkedUserGameRoleService.update(executedUserId, LinkedUserGameRole::isExecuted, true);
		this.pusherModule.trigger(channelName, "playerKilled", Collections.singletonMap("userId", executedUserId));

		return ResponseEntity.ok(Collections.emptyMap());
	}

	/**
	 * Investigates a user's party membership. This is an executive action.
	 *
	 * @param investigatedUserId The userId of the user to investigate.
	 * @param channelName        The channelName of the game to investigate in.
	 * @return The name of the party which the user belongs to.
	 */
	@GetMapping(value = "/investigate/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> investigateLoyalty(@PathVariable("userId") long investigatedUserId, @RequestParam("channelName") String channelName, @RequestParam("userId") long userId, @RequestHeader(HttpHeaders.AUTHORIZATION) String base64Token) {
		if (channelName == null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		var game = this.gameService.getByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException(String.format("No game was found for the channelName '%s'.", channelName)));
		if (!this.eligibilityModule.isLoyaltyInvestigationEligible(game)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Loyalty investigation is not available in the current game."));
		}

		if (!this.linkedUserGameRoleService.hasValidToken(userId, base64Token)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Invalid authorization."));
		}

		var gameId = game.getId();
		var userRoleId = this.linkedUserGameRoleService.getSingle(x -> x.getId() == investigatedUserId && x.getGameId() == gameId && !x.isExecuted()).project(LinkedUserGameRole::getRoleId).first().orElseThrow(() -> new EmptyOptionalException("Player was not found in the current game."));
		if (userRoleId == RoleTypes.FASCIST.getId() || investigatedUserId == RoleTypes.SECRET_HITLER.getId()) {
			return ResponseEntity.ok(Collections.singletonMap("party", RoleTypes.FASCIST.getName()));
		}

		return ResponseEntity.ok(Collections.singletonMap("party", RoleTypes.LIBERAL.getName()));
	}
}
