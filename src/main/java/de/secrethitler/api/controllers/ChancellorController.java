package de.secrethitler.api.controllers;

import com.github.collinalpert.java2db.queries.OrderTypes;
import com.github.collinalpert.lambda2sql.functions.SqlFunction;
import de.secrethitler.api.entities.Game;
import de.secrethitler.api.entities.LinkedRoundPolicySuggestion;
import de.secrethitler.api.entities.LinkedUserGameRole;
import de.secrethitler.api.entities.Round;
import de.secrethitler.api.entities.Vote;
import de.secrethitler.api.enums.PolicyTypes;
import de.secrethitler.api.enums.RoleTypes;
import de.secrethitler.api.exceptions.EmptyOptionalException;
import de.secrethitler.api.modules.LoggingModule;
import de.secrethitler.api.modules.NumberModule;
import de.secrethitler.api.modules.PolicyModule;
import de.secrethitler.api.modules.PusherModule;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.LinkedRoundPolicySuggestionService;
import de.secrethitler.api.services.LinkedUserGameRoleService;
import de.secrethitler.api.services.RoundService;
import de.secrethitler.api.services.VoteService;
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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/chancellor")
@CrossOrigin(allowCredentials = "true")
public class ChancellorController {

	private final GameService gameService;
	private final RoundService roundService;
	private final VoteService voteService;
	private final PusherModule pusherModule;
	private final LinkedUserGameRoleService linkedUserGameRoleService;
	private final PolicyModule policyModule;
	private final LinkedRoundPolicySuggestionService linkedRoundPolicySuggestionService;
	private final LoggingModule logger;
	private final NumberModule numberModule;

	public ChancellorController(GameService gameService,
								RoundService roundService,
								VoteService voteService,
								PusherModule pusherModule,
								LinkedUserGameRoleService linkedUserGameRoleService,
								PolicyModule policyModule,
								LinkedRoundPolicySuggestionService linkedRoundPolicySuggestionService,
								LoggingModule logger,
								NumberModule numberModule) {
		this.gameService = gameService;
		this.roundService = roundService;
		this.voteService = voteService;
		this.pusherModule = pusherModule;
		this.linkedUserGameRoleService = linkedUserGameRoleService;
		this.policyModule = policyModule;
		this.linkedRoundPolicySuggestionService = linkedRoundPolicySuggestionService;
		this.logger = logger;
		this.numberModule = numberModule;
	}

	@PostMapping(value = "/nominate", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> nominateChancellor(@RequestBody Map<String, Object> requestBody) throws SQLException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		if (!requestBody.containsKey("chancellorId")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "chancellorId is missing."));
		}

		var channelName = (String) requestBody.get("channelName");
		var chancellorId = this.numberModule.getAsLong(requestBody.get("chancellorId"));

		long gameId = this.gameService.getIdByChannelName(channelName).orElseThrow(() -> new EmptyOptionalException("No game was found for the given channelName."));

		// Get the last two rounds. The first one will be the current round, while the second one will be the previous round.
		var previousRounds = this.roundService.getMultiple(x -> x.getGameId() == gameId).orderBy(OrderTypes.DESCENDING, Round::getId).limit(2).toList();
		if (previousRounds.isEmpty()) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "No rounds were found for the given channelName"));
		}

		if (previousRounds.get(0).getNominatedChancellorId() != null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Chancellor has already been nominated for this round."));
		}

		// Get the previous round. Index 0 will be the current round which is why we need index 1.
		if (!previousRounds.get(0).isSpecialElectionRound() && previousRounds.size() > 1 && previousRounds.get(1).getChancellorId() != null && (previousRounds.get(1).getChancellorId() == chancellorId || previousRounds.get(1).getPresidentId() == chancellorId)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Nominated chancellor was either president or chancellor in the previous round."));
		}

		// Update the current round with the nominee.
		this.roundService.update(previousRounds.get(0).getId(), Round::getNominatedChancellorId, chancellorId);

		this.pusherModule.trigger(channelName, "chancellor_nominated", Collections.singletonMap("chancellorId", chancellorId));

		return ResponseEntity.ok(Collections.emptyMap());
	}

	@PostMapping(value = "/vote", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> voteChancellor(@RequestBody Map<String, Object> requestBody, HttpSession session) throws ExecutionException, InterruptedException, SQLException {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		if (!requestBody.containsKey("votedYes")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Vote is missing."));
		}

		var channelName = (String) requestBody.get("channelName");
		var votedYes = (boolean) requestBody.get("votedYes");
		var userId = (long) session.getAttribute("userId");

		if (!this.linkedUserGameRoleService.any(x -> x.getId() == userId && !x.isExecuted())) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("message", "User was not found in session."));
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
		pusher.trigger(channelName, "chancellor_vote", Map.of("user_id", userId, "voted_yes", votedYes));

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

			pusher.trigger(channelName, "chancellor_elected", Collections.singletonMap("elected", chancellorElected));

			if (chancellorElected) {
				// Set the chancellor to the nominated chancellor in the database.
				this.roundService.update(currentRoundId, (SqlFunction<Round, Long>) Round::getChancellorId, (SqlFunction<Round, Long>) Round::getNominatedChancellorId);

				// Since the chancellor was elected, the nominated chancellor is also the elected one.
				long electedChancellorId = Optional.ofNullable(currentRound.getNominatedChancellorId()).orElseThrow(() -> new EmptyOptionalException("No chancellor was nominated for this round."));

				boolean isHitler = this.linkedUserGameRoleService.getSingle(x -> x.getId() == electedChancellorId && x.getGameId() == gameId && !x.isExecuted()).project(LinkedUserGameRole::getRoleId).first().map(roleId -> roleId == RoleTypes.SECRET_HITLER.getId()).orElseThrow(() -> new EmptyOptionalException("Chancellor was not found in game-link."));
				var fascistPolicyId = PolicyTypes.FASCIST.getId();
				if (isHitler && this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == fascistPolicyId) >= 3) {
					pusher.trigger(channelName, "game_won", Map.of("party", RoleTypes.FASCIST.getName(), "reason", "Hitler was elected chancellor!"));

					return ResponseEntity.ok(Collections.emptyMap());
				}

				// Since the election was successful, give the president policies to choose from.
				var policies = this.policyModule.drawPolicies(gameId, 3);
				pusher.trigger(String.format("private-%d", currentPresidentId), "president_receive_policies", Collections.singletonMap("policies", new String[]{policies[0].getName(), policies[1].getName(), policies[2].getName()}));

				this.linkedRoundPolicySuggestionService.create(new LinkedRoundPolicySuggestion(currentRoundId, policies[0].getId()),
						new LinkedRoundPolicySuggestion(currentRoundId, policies[1].getId()),
						new LinkedRoundPolicySuggestion(currentRoundId, policies[2].getId()));
			} else {
				// Perform election tracker handling.
				int electionTrackings = this.gameService.getSingle(x -> x.getId() == gameId).project(Game::getElectionTrackings).first().orElseThrow(() -> new EmptyOptionalException("No game found for election tracking."));
				var failedRounds = this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == null);
				if (failedRounds >= (electionTrackings * 3) + 3) {
					var policyToEnact = this.policyModule.drawPolicies(gameId, 1);
					var roundTask = this.roundService.updateAsync(currentRoundId, Round::getEnactedPolicyId, policyToEnact[0].getId(), logger::log);
					var gameTask = this.gameService.updateAsync(gameId, (SqlFunction<Game, Integer>) Game::getElectionTrackings, (SqlFunction<Game, Integer>) game -> game.getElectionTrackings() + 1, logger::log);
					pusher.trigger(channelName, "election_tracker", Collections.emptyMap());
					pusher.trigger(channelName, "policy_enacted", Collections.singletonMap("policy", policyToEnact[0].getName()));

					CompletableFuture.allOf(roundTask, gameTask).get();
				}
			}
		}

		return ResponseEntity.ok(Collections.emptyMap());
	}
}
