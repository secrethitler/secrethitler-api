package de.secrethitler.api.controllers;

import de.secrethitler.api.entities.Round;
import de.secrethitler.api.enums.PolicyTypes;
import de.secrethitler.api.enums.RoleTypes;
import de.secrethitler.api.exceptions.EmptyOptionalException;
import de.secrethitler.api.modules.EligibilityModule;
import de.secrethitler.api.modules.NumberModule;
import de.secrethitler.api.modules.PolicyModule;
import de.secrethitler.api.modules.PusherModule;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.LinkedRoundPolicySuggestionService;
import de.secrethitler.api.services.LinkedUserGameRoleService;
import de.secrethitler.api.services.RoundService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/policy")
@CrossOrigin(allowCredentials = "true")
public class PolicyController {

	private final PolicyModule policyModule;
	private final PusherModule pusherModule;
	private final GameService gameService;
	private final RoundService roundService;
	private final LinkedRoundPolicySuggestionService linkedRoundPolicySuggestionService;
	private final EligibilityModule eligibilityModule;
	private final NumberModule numberModule;
	private final LinkedUserGameRoleService linkedUserGameRoleService;

	public PolicyController(PolicyModule policyModule, PusherModule pusherModule,
							GameService gameService,
							RoundService roundService,
							LinkedRoundPolicySuggestionService linkedRoundPolicySuggestionService,
							EligibilityModule eligibilityModule,
							NumberModule numberModule,
							LinkedUserGameRoleService linkedUserGameRoleService) {
		this.policyModule = policyModule;
		this.pusherModule = pusherModule;
		this.gameService = gameService;
		this.roundService = roundService;
		this.linkedRoundPolicySuggestionService = linkedRoundPolicySuggestionService;
		this.eligibilityModule = eligibilityModule;
		this.numberModule = numberModule;
		this.linkedUserGameRoleService = linkedUserGameRoleService;
	}

	/**
	 * Handles the selection of the president's choice of policies for a round.
	 *
	 * @param requestBody The request's body, containing the channelName of the game and the name of the policy the president chose to discard.
	 * @return A successful 200 HTTP response.
	 * @throws ExecutionException   The exception which can occur when performing asynchronous operations.
	 * @throws InterruptedException The exception which can occur when performing asynchronous operations.
	 */
	@PostMapping(value = "/president-pick", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> presidentPickPolicy(@RequestBody Map<String, Object> requestBody, @RequestHeader(HttpHeaders.AUTHORIZATION) String base64Token) throws ExecutionException, InterruptedException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		if (!requestBody.containsKey("discardedPolicy")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "discardedPolicy is missing."));
		}

		if (!requestBody.containsKey("userId")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "userId is missing."));
		}

		var channelName = (String) requestBody.get("channelName");
		var discardedPolicyName = (String) requestBody.get("discardedPolicy");
		var userId = this.numberModule.getAsLong(requestBody.get("userId"));

		if (!this.linkedUserGameRoleService.hasValidToken(userId, base64Token)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Invalid authorization."));
		}

		var currentRound = discardPolicy(channelName, discardedPolicyName, userId);
		if (currentRound == null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "User can't discard policy!"));
		}

		var roundId = currentRound.getId();

		if (currentRound.getChancellorId() == null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "No chancellor was found in the current round."));
		}

		long chancellorId = currentRound.getChancellorId();

		// Notify the chancellor of the remaining policies.
		var remainingPolicies = this.linkedRoundPolicySuggestionService.getMultiple(x -> x.getRoundId() == roundId && !x.isDiscarded()).toStream().map(x -> x.getPolicyType().getName()).toArray(String[]::new);
		this.pusherModule.trigger(String.format("private-%d", chancellorId), "chancellorReceivePolicies", Collections.singletonMap("policies", remainingPolicies));

		return ResponseEntity.ok(Collections.emptyMap());
	}

	/**
	 * Handles the selection of the chancellor's choice of the policy for a round.
	 * Also contains the game-over handling, if the Fascists hereby enacted six fascist policies or the Liberals enacted five liberal policies.
	 *
	 * @param requestBody The request's body, containing the channelName of the game and the name of the policy the chancellor chose to discard.
	 * @return A successful 200 HTTP response.
	 * @throws ExecutionException   The exception which can occur when performing asynchronous operations.
	 * @throws InterruptedException The exception which can occur when performing asynchronous operations.
	 * @throws SQLException         The exception which can occur when interchanging with the database.
	 */
	@PostMapping(value = "/chancellor-pick", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> chancellorPickPolicy(@RequestBody Map<String, Object> requestBody, @RequestHeader(HttpHeaders.AUTHORIZATION) String base64Token) throws ExecutionException, InterruptedException, SQLException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		if (!requestBody.containsKey("discardedPolicy")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "discardedPolicy is missing."));
		}

		if (!requestBody.containsKey("userId")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "userId is missing."));
		}

		var channelName = (String) requestBody.get("channelName");
		var discardedPolicyName = (String) requestBody.get("discardedPolicy");
		var userId = this.numberModule.getAsLong(requestBody.get("userId"));

		if (!linkedUserGameRoleService.hasValidToken(userId, base64Token)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Invalid authorization."));
		}

		var currentRound = discardPolicy(channelName, discardedPolicyName, userId);
		if (currentRound == null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "User can't discard policy!"));
		}

		var currentRoundId = currentRound.getId();

		var remainingPolicyLinks = this.linkedRoundPolicySuggestionService.getMultiple(x -> x.getRoundId() == currentRoundId && !x.isDiscarded()).toArray();
		if (remainingPolicyLinks.length != 1) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", String.format("%d policies to enact were found.", remainingPolicyLinks.length)));
		}

		this.roundService.update(currentRoundId, Round::getEnactedPolicyId, remainingPolicyLinks[0].getPolicyId());

		var pusher = this.pusherModule.getPusherInstance();
		pusher.trigger(channelName, "policyEnacted", Collections.singletonMap("policy", remainingPolicyLinks[0].getPolicyType().getName()));

		var gameId = currentRound.getGameId();

		var fascistPolicyId = PolicyTypes.FASCIST.getId();
		var liberalPolicyId = PolicyTypes.LIBERAL.getId();
		if (remainingPolicyLinks[0].getPolicyId() == fascistPolicyId) {
			if (this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == fascistPolicyId) >= 6) {
				pusher.trigger(channelName, "gameWon", Map.of("party", RoleTypes.FASCIST.getName(), "reason", "The Fascists enacted six fascist policies!"));

				return ResponseEntity.ok(Collections.emptyMap());
			}

			this.policyModule.decrementFascistPolicyCountAsync(currentRound.getGameId()).get();

			// If a fascist policy was enacted, check if an executive action was unlocked.
			checkForExecutiveAction(currentRound);
		} else if (remainingPolicyLinks[0].getPolicyId() == liberalPolicyId) {
			if (this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == liberalPolicyId) >= 5) {
				pusher.trigger(channelName, "gameWon", Map.of("party", RoleTypes.LIBERAL.getName(), "reason", "The Liberals enacted five liberal policies!"));

				return ResponseEntity.ok(Collections.emptyMap());
			}

			this.policyModule.decrementLiberalPolicyCountAsync(currentRound.getGameId()).get();
		}

		return ResponseEntity.ok(Collections.emptyMap());
	}

	/**
	 * Allows the president of the current round to preview (peek) the next three policies which will be displayed to the president in the next round. This is an executive action.
	 *
	 * @param channelName The channelName of the game to perform the policy peek in.
	 * @return The names of the topmost three policies in the card deck.
	 * @throws SQLException The exception which can occur when interchanging with the database.
	 */
	@GetMapping(value = "/peek", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> policyPeek(@RequestParam("channelName") String channelName, @RequestParam("userId") long userId, @RequestHeader(HttpHeaders.AUTHORIZATION) String base64Token) throws SQLException {
		if (!this.linkedUserGameRoleService.hasValidToken(userId, base64Token)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Invalid authorization."));
		}

		long gameId = this.gameService.getIdByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException(String.format("No game was found for the channelName '%s'.", channelName)));
		var currentRound = this.roundService.getCurrentRound(gameId).orElseThrow(() -> new EmptyOptionalException(String.format("No round was found for the channelName '%s'.", channelName)));
		if (currentRound.getPresidentId() != userId) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Only the president can peek."));
		}

		// Check if the current game is eligible to perform a policy peek.
		if (!this.eligibilityModule.isPolicyPeekEligible(currentRound.getGame())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("message", "Policy peek is not allowed in the current state of the game."));
		}

		var policyNames = Arrays.stream(this.policyModule.drawPolicies(gameId, 3)).map(PolicyTypes::getName).collect(Collectors.toList());
		return ResponseEntity.ok(Collections.singletonMap("policies", policyNames));
	}

	/**
	 * Checks if the current state of the game needs the president to perform an executive action and notifies him if so.
	 *
	 * @param round The round the game is currently in.
	 */
	private void checkForExecutiveAction(Round round) {
		var executiveAction = this.eligibilityModule.getExecutiveAction(round.getGame());
		if (executiveAction == null) {
			return;
		}

		this.pusherModule.trigger(String.format("private-%d", round.getPresidentId()), executiveAction.getPusherEventName(), Collections.emptyMap());
	}

	/**
	 * Discards a policy by name.
	 *
	 * @param channelName         The channelName of the game to discard the policy in.
	 * @param discardedPolicyName The name of the policy to discard.
	 * @return The round the game is currently in.
	 * @throws ExecutionException   The exception which can occur when performing asynchronous operations.
	 * @throws InterruptedException The exception which can occur when performing asynchronous operations.
	 */
	private Round discardPolicy(String channelName, String discardedPolicyName, long userId) throws InterruptedException, ExecutionException {
		long gameId = this.gameService.getIdByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException(String.format("No game was found for the channelName '%s'.", channelName)));
		var currentRound = this.roundService.getCurrentRound(gameId).orElseThrow(() -> new EmptyOptionalException("No round was found for the current game."));
		if (currentRound.getPresidentId() != userId || currentRound.getChancellorId() != userId) {
			return null;
		}

		// Discard the president's choice.
		var discardedPolicy = this.policyModule.getByName(discardedPolicyName);
		this.policyModule.discardPolicy(discardedPolicy, gameId, currentRound.getId());

		return currentRound;
	}
}
