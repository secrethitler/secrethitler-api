package de.secrethitler.api.controllers;

import com.github.collinalpert.java2db.queries.OrderTypes;
import com.github.collinalpert.lambda2sql.functions.SqlFunction;
import de.secrethitler.api.entities.LinkedRoundPolicySuggestion;
import de.secrethitler.api.entities.LinkedUserGameRole;
import de.secrethitler.api.entities.Round;
import de.secrethitler.api.entities.Vote;
import de.secrethitler.api.enums.PolicyTypes;
import de.secrethitler.api.enums.RoleTypes;
import de.secrethitler.api.exceptions.EmptyOptionalException;
import de.secrethitler.api.modules.ElectionTrackerModule;
import de.secrethitler.api.modules.EligibilityModule;
import de.secrethitler.api.modules.NumberModule;
import de.secrethitler.api.modules.PolicyModule;
import de.secrethitler.api.modules.PusherModule;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.LinkedRoundPolicySuggestionService;
import de.secrethitler.api.services.LinkedUserGameRoleService;
import de.secrethitler.api.services.RoundService;
import de.secrethitler.api.services.VoteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/chancellor")
@CrossOrigin(allowCredentials = "true")
public class ChancellorController {

	private final GameService gameService;
	private final RoundService roundService;
	private final VoteService voteService;
	private final PusherModule pusherModule;
	private final LinkedUserGameRoleService linkedUserGameRoleService;
	private final PolicyModule policyModule;
	private final LinkedRoundPolicySuggestionService linkedRoundPolicySuggestionService;
	private final NumberModule numberModule;
	private final EligibilityModule eligibilityModule;
	private final ElectionTrackerModule electionTrackerModule;

	public ChancellorController(GameService gameService,
								RoundService roundService,
								VoteService voteService,
								PusherModule pusherModule,
								LinkedUserGameRoleService linkedUserGameRoleService,
								PolicyModule policyModule,
								LinkedRoundPolicySuggestionService linkedRoundPolicySuggestionService,
								NumberModule numberModule,
								EligibilityModule eligibilityModule,
								ElectionTrackerModule electionTrackerModule) {
		this.gameService = gameService;
		this.roundService = roundService;
		this.voteService = voteService;
		this.pusherModule = pusherModule;
		this.linkedUserGameRoleService = linkedUserGameRoleService;
		this.policyModule = policyModule;
		this.linkedRoundPolicySuggestionService = linkedRoundPolicySuggestionService;
		this.numberModule = numberModule;
		this.eligibilityModule = eligibilityModule;
		this.electionTrackerModule = electionTrackerModule;
	}

	/**
	 * Nominates a chancellor. Checks if the president is eligible to be chancellor in the current round.
	 *
	 * @param requestBody The request's body, containing the channelName and the chancellorId which is supposed to be nominated.
	 * @return A successful 200 HTTP response.
	 * @throws SQLException The exception which can occur when interchanging with the database.
	 */
	@PostMapping(value = "/nominate", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> nominateChancellor(@RequestBody Map<String, Object> requestBody, @RequestHeader(HttpHeaders.AUTHORIZATION) String base64Token) throws SQLException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		if (!requestBody.containsKey("chancellorId")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "chancellorId is missing."));
		}

		if (!requestBody.containsKey("userId")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "userId is missing."));
		}

		var channelName = (String) requestBody.get("channelName");
		var chancellorId = this.numberModule.getAsLong(requestBody.get("chancellorId"));
		var userId = this.numberModule.getAsLong(requestBody.get("userId"));

		if (!this.linkedUserGameRoleService.hasValidToken(userId, base64Token)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Invalid authorization."));
		}

		long gameId = this.gameService.getIdByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException("No game was found for the given channelName."));

		// Get the last two rounds. The first one will be the current round, while the second one will be the previous round.
		var previousRounds = this.roundService.getMultiple(x -> x.getGameId() == gameId).orderBy(OrderTypes.DESCENDING, Round::getSequenceNumber).limit(2).toList();
		if (previousRounds.isEmpty()) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "No rounds were found for the given channelName"));
		}

		if (previousRounds.get(0).getPresidentId() != userId) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Request is not coming from current president."));
		}

		if (previousRounds.get(0).getNominatedChancellorId() != null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Chancellor has already been nominated for this round."));
		}

		// Get the previous round. Index 0 will be the current round which is why we need index 1.
		if (!previousRounds.get(0).isSpecialElectionRound() && previousRounds.size() > 1 && !this.eligibilityModule.isChancellorEligible(previousRounds.get(1), chancellorId)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Nominated chancellor was either president or chancellor in the previous round."));
		}

		// Update the current round with the nominee.
		this.roundService.update(previousRounds.get(0).getId(), Round::getNominatedChancellorId, chancellorId);

		this.pusherModule.trigger(channelName, "chancellorNominated", Collections.singletonMap("chancellorId", chancellorId));

		return ResponseEntity.ok(Collections.emptyMap());
	}

	/**
	 * Processes a player's vote for the nominated chancellor.
	 * Contains election tracker handling if the vote fails and sends the president the three policies if the vote succeeds.
	 * Also contains the game-over handling, if Hitler is elected chancellor.
	 *
	 * @param requestBody The request's body, containing the channelName and if the player voted in favor of the chancellor or not.
	 * @return A successful 200 HTTP response.
	 * @throws ExecutionException   The exception which can occur when performing asynchronous operations.
	 * @throws InterruptedException The exception which can occur when performing asynchronous operations.
	 * @throws SQLException         The exception which can occur when interchanging with the database.
	 */
	@PostMapping(value = "/vote", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> voteChancellor(@RequestBody Map<String, Object> requestBody, @RequestHeader(value = HttpHeaders.AUTHORIZATION) String base64Token) throws ExecutionException, InterruptedException, SQLException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		if (!requestBody.containsKey("votedYes")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Vote is missing."));
		}

		if (!requestBody.containsKey("userId")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "userId is missing."));
		}

		var channelName = (String) requestBody.get("channelName");
		var votedYes = (boolean) requestBody.get("votedYes");
		var userId = this.numberModule.getAsLong(requestBody.get("userId"));

		if (!this.linkedUserGameRoleService.hasValidToken(userId, base64Token)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("message", "Invalid authorization."));
		}

		long gameId = this.gameService.getIdByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException("No game was found for the given channelName."));

		var currentRound = this.roundService.getCurrentRound(gameId).orElseThrow(() -> new EmptyOptionalException("No round was found for the given channelName."));

		if (currentRound.getChancellorId() != null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Chancellor has already been elected."));
		}

		var currentRoundId = currentRound.getId();
		var currentPresidentId = currentRound.getPresidentId();

		if (this.voteService.any(x -> x.getRoundId() == currentRoundId && x.getUserId() == userId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("message", "The user has already voted for this round."));
		}

		this.voteService.create(new Vote(userId, currentRoundId, votedYes));

		var pusher = this.pusherModule.getPusherInstance();
		pusher.trigger(channelName, "chancellorVote", Map.of("userId", userId, "votedYes", votedYes));

		var numberOfPlayers = this.linkedUserGameRoleService.count(x -> x.getGameId() == gameId && !x.isExecuted());
		var votes = this.voteService.count(x -> x.getRoundId() == currentRoundId);

		if (votes >= numberOfPlayers) {
			var numberOfYesVotes = (double) this.voteService.count(x -> x.getRoundId() == currentRoundId && x.getVotedForChancellor());
			var breakingPoint = ((double) votes) / 2;
			var chancellorElected = false;
			if (numberOfYesVotes > breakingPoint) {
				chancellorElected = true;
			} else if (numberOfYesVotes == breakingPoint) {
				// In this case, the vote of the president breaks the tie.
				var presidentVotedYesOptional = this.voteService.getSingle(x -> x.getRoundId() == currentRoundId && x.getUserId() == currentPresidentId).project(Vote::getVotedForChancellor).first();
				chancellorElected = presidentVotedYesOptional.orElseThrow(() -> new EmptyOptionalException("The president has not voted in the current round yet."));
			}

			pusher.trigger(channelName, "chancellorElected", Collections.singletonMap("elected", chancellorElected));

			if (chancellorElected) {
				// Set the chancellor to the nominated chancellor in the database.
				this.roundService.update(currentRoundId, (SqlFunction<Round, Long>) Round::getChancellorId, (SqlFunction<Round, Long>) Round::getNominatedChancellorId);

				// Since the chancellor was elected, the nominated chancellor is also the elected one.
				long electedChancellorId = Optional.ofNullable(currentRound.getNominatedChancellorId()).orElseThrow(() -> new EmptyOptionalException("No chancellor was nominated for this round."));

				boolean isHitler = this.linkedUserGameRoleService.getSingle(x -> x.getId() == electedChancellorId && x.getGameId() == gameId && !x.isExecuted()).project(LinkedUserGameRole::getRoleId).first().map(roleId -> roleId == RoleTypes.SECRET_HITLER.getId()).orElseThrow(() -> new EmptyOptionalException("Chancellor was not found in game-link."));
				var fascistPolicyId = PolicyTypes.FASCIST.getId();
				if (isHitler && this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == fascistPolicyId) >= 3) {
					pusher.trigger(channelName, "gameWon", Map.of("party", RoleTypes.FASCIST.getName(), "reason", "Hitler was elected chancellor!"));

					return ResponseEntity.ok(Collections.emptyMap());
				}

				// Since the election was successful, give the president policies to choose from.
				var policies = this.policyModule.drawPolicies(gameId, 3);
				pusher.trigger(String.format("private-%d", currentPresidentId), "presidentReceivePolicies", Collections.singletonMap("policies", new String[]{policies[0].getName(), policies[1].getName(), policies[2].getName()}));

				this.linkedRoundPolicySuggestionService.create(new LinkedRoundPolicySuggestion(currentRoundId, policies[0].getId()),
						new LinkedRoundPolicySuggestion(currentRoundId, policies[1].getId()),
						new LinkedRoundPolicySuggestion(currentRoundId, policies[2].getId()));
			} else {
				this.electionTrackerModule.enactPolicyIfNecessary(channelName, gameId, currentRoundId);
			}
		}

		return ResponseEntity.ok(Collections.emptyMap());
	}
}
