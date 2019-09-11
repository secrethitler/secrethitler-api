package de.secrethitler.api.controllers;

import de.secrethitler.api.entities.Game;
import de.secrethitler.api.entities.Round;
import de.secrethitler.api.enums.PolicyTypes;
import de.secrethitler.api.exceptions.EmptyOptionalException;
import de.secrethitler.api.modules.PolicyModule;
import de.secrethitler.api.modules.PusherModule;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.LinkedRoundPolicySuggestionService;
import de.secrethitler.api.services.RoundService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/policy")
@CrossOrigin(origins = {"http://10.14.221.66", "http://localhost", "http://localhost:8080", "https://secret-hitler.netlify.com", "https://geheimerdeutscher.tk"}, allowCredentials = "true")
public class PolicyController {

	private final PolicyModule policyModule;
	private final PusherModule pusherModule;
	private final GameService gameService;
	private final RoundService roundService;
	private final LinkedRoundPolicySuggestionService linkedRoundPolicySuggestionService;

	public PolicyController(PolicyModule policyModule, PusherModule pusherModule,
							GameService gameService,
							RoundService roundService,
							LinkedRoundPolicySuggestionService linkedRoundPolicySuggestionService) {
		this.policyModule = policyModule;
		this.pusherModule = pusherModule;
		this.gameService = gameService;
		this.roundService = roundService;
		this.linkedRoundPolicySuggestionService = linkedRoundPolicySuggestionService;
	}

	@PostMapping(value = "/president-pick", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> presidentPickPolicy(@RequestBody Map<String, Object> requestBody) throws ExecutionException, InterruptedException, SQLException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		if (!requestBody.containsKey("discardedPolicy")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "discardedPolicy is missing."));
		}

		var channelName = (String) requestBody.get("channelName");
		var discardedPolicyName = (String) requestBody.get("discardedPolicy");

		var currentRound = discardPolicy(channelName, discardedPolicyName);

		if (currentRound.getChancellorId() == null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "No chancellor was found in the current round."));
		}

		long chancellorId = currentRound.getChancellorId();

		// Notify the chancellor of the president's choice of discarded policy.
		this.pusherModule.trigger(String.format("private-%d", chancellorId), "president_pick", Collections.singletonMap("policy", discardedPolicyName));

		return ResponseEntity.ok(Collections.emptyMap());
	}

	@PostMapping(value = "/chancellor-pick", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> chancellorPickPolicy(@RequestBody Map<String, Object> requestBody) throws ExecutionException, InterruptedException, SQLException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		if (!requestBody.containsKey("discardedPolicy")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "discardedPolicy is missing."));
		}

		var channelName = (String) requestBody.get("channelName");
		var discardedPolicyName = (String) requestBody.get("discardedPolicy");

		var currentRound = discardPolicy(channelName, discardedPolicyName);
		var currentRoundId = currentRound.getId();

		var remainingPolicyLinks = this.linkedRoundPolicySuggestionService.getMultiple(x -> x.getRoundId() == currentRoundId && x.isDiscarded()).toArray();
		if (remainingPolicyLinks.length != 1) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", String.format("%d policies to enact were found.", remainingPolicyLinks.length)));
		}

		this.roundService.update(currentRoundId, Round::getEnactedPolicyId, remainingPolicyLinks[0].getPolicyId());

		this.pusherModule.trigger(channelName, "policy_enacted", Collections.singletonMap("policy", remainingPolicyLinks[0].getPolicyType().getName()));

		if (remainingPolicyLinks[0].getPolicyId() == PolicyTypes.FASCIST.getId()) {
			this.policyModule.decrementFascistPolicyCountAsync(currentRound.getGameId()).get();

			// If a fascist policy was enacted, check is an executive action was unlocked.
			checkForExecutiveAction(currentRound);
		} else if (remainingPolicyLinks[0].getPolicyId() == PolicyTypes.LIBERAL.getId()) {
			this.policyModule.decrementLiberalPolicyCountAsync(currentRound.getGameId()).get();
		}

		return ResponseEntity.ok(Collections.emptyMap());
	}

	@GetMapping(value = "/peek", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> policyPeek(@RequestParam("channelName") String channelName) throws ExecutionException, InterruptedException {
		long gameId = this.gameService.getIdByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException(String.format("No game was found for the channelName '%s'.", channelName)));
		var currentRound = this.roundService.getCurrentRound(gameId).orElseThrow(() -> new EmptyOptionalException(String.format("No round was found for the channelName '%s'.", channelName)));

		// Check if the current game is eligible to perform a policy peek.
		if (!policyPeekEligible(currentRound.getGame())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("message", "Policy peek is not allowed in the current state of the game."));
		}

		return ResponseEntity.ok(Collections.singletonMap("policies", this.policyModule.drawPolicies(gameId, 3)));
	}

	private void checkForExecutiveAction(Round round) {
		if (policyPeekEligible(round.getGame())) {
			this.pusherModule.trigger(String.format("private-%d", round.getPresidentId()), "policy_peek", Collections.emptyMap());
		}
	}

	private Round discardPolicy(String channelName, String discardedPolicyName) throws InterruptedException, ExecutionException {
		long gameId = this.gameService.getIdByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException(String.format("No game was found for the channelName '%s'.", channelName)));
		var currentRound = this.roundService.getCurrentRound(gameId).orElseThrow(() -> new EmptyOptionalException("No round was found for the current game."));

		// Discard the president's choice.
		var discardedPolicy = this.policyModule.getByName(discardedPolicyName);
		this.policyModule.discardPolicy(discardedPolicy, gameId, currentRound.getId());

		return currentRound;
	}

	private boolean policyPeekEligible(Game game) {
		var gameId = game.getId();
		var fascistId = PolicyTypes.FASCIST.getId();
		return game.getInitialPlayerCount() >= 5 &&
				game.getInitialPlayerCount() <= 6 &&
				this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == fascistId) == 3;
	}

}
