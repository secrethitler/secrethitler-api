package de.secrethitler.api.controllers;

import com.github.collinalpert.java2db.queries.OrderTypes;
import de.secrethitler.api.entities.Game;
import de.secrethitler.api.entities.Round;
import de.secrethitler.api.entities.Vote;
import de.secrethitler.api.modules.LoggingModule;
import de.secrethitler.api.modules.PolicyModule;
import de.secrethitler.api.modules.PusherModule;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.LinkedUserGameRoleService;
import de.secrethitler.api.services.RoundService;
import de.secrethitler.api.services.VoteService;
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
	private final VoteService voteService;
	private final PusherModule pusherModule;
	private final LinkedUserGameRoleService linkedUserGameRoleService;
	private final PolicyModule policyModule;

	public ChancellorController(GameService gameService,
								RoundService roundService,
								LoggingModule logger,
								VoteService voteService,
								PusherModule pusherModule,
								LinkedUserGameRoleService linkedUserGameRoleService,
								PolicyModule policyModule) {
		this.gameService = gameService;
		this.roundService = roundService;
		this.logger = logger;
		this.voteService = voteService;
		this.pusherModule = pusherModule;
		this.linkedUserGameRoleService = linkedUserGameRoleService;
		this.policyModule = policyModule;
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
		// Get the last two rounds. The first one will be the current round, while the second one will be the previous round.
		var previousRounds = this.roundService.getMultiple(x -> x.getGameId() == gameId).orderBy(OrderTypes.DESCENDING, Round::getId).limit(2).toList();
		if (previousRounds.isEmpty()) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "No rounds were found for the given channelName"));
		}

		// Get the previous round. Index 0 will be the current round which is why we need index 1.
		if (previousRounds.size() > 1 && (previousRounds.get(1).getChancellorId() == chancellorId || previousRounds.get(1).getPresidentId() == chancellorId)) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Nominated chancellor was either president or chancellor in the previous round."));
		}

		try {
			// Update the current round with the nominee.
			this.roundService.update(previousRounds.get(0).getId(), Round::getNominatedChancellorId, chancellorId);
		} catch (SQLException e) {
			e.printStackTrace();
			this.logger.log(e);
			return ResponseEntity.unprocessableEntity().body(Collections.singletonMap("message", e.getMessage()));
		}

		var pusher = this.pusherModule.getPusherInstance();
		pusher.trigger(channelName, "chancellor_nominated", Collections.singletonMap("chancellor_id", chancellorId));

		return ResponseEntity.ok(Collections.emptyMap());
	}

	@PostMapping(value = "/vote", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> voteChancellor(@RequestBody Map<String, Object> requestBody, HttpSession session) {
		if (!requestBody.containsKey("channelName")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "channelName is missing."));
		}

		if (!requestBody.containsKey("votedYes")) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Vote is missing."));
		}

		var channelName = (String) requestBody.get("channelName");
		var votedYes = (boolean) requestBody.get("votedYes");
		var userId = (long) session.getAttribute("userId");

		var gameIdOptional = this.gameService.getSingle(x -> x.getChannelName() == channelName).project(Game::getId).first();
		if (gameIdOptional.isEmpty()) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "No game was found for the given channelName."));
		}

		var gameId = gameIdOptional.get();
		var currentRoundOptional = this.roundService.getMultiple(x -> x.getGameId() == gameId).orderBy(OrderTypes.DESCENDING, Round::getSequenceNumber).limit(1).first();
		if (currentRoundOptional.isEmpty()) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("message", "No round was found for the given channelName."));
		}

		var currentRoundId = currentRoundOptional.get().getId();
		var currentPresidentId = currentRoundOptional.get().getPresidentId();
		var vote = new Vote(userId, currentRoundId, votedYes);

		try {
			this.voteService.create(vote);
		} catch (SQLException e) {
			e.printStackTrace();
			this.logger.log(e);
			return ResponseEntity.unprocessableEntity().body(Collections.singletonMap("message", e.getMessage()));
		}

		var pusher = this.pusherModule.getPusherInstance();
		pusher.trigger(channelName, "chancellor_vote", Map.of("user_id", userId, "voted_yes", votedYes));

		var numberOfPlayers = this.linkedUserGameRoleService.count(x -> x.getGameId() == gameId);
		var votes = this.voteService.count(x -> x.getRoundId() == currentRoundId);

		if (votes >= numberOfPlayers) {
			var numberOfYesVotes = (double) this.voteService.count(x -> x.getRoundId() == currentRoundId && x.getVotedChancellor());
			var breakingPoint = ((double) votes) / 2;
			var chancellorElected = false;
			if (numberOfYesVotes > breakingPoint) {
				chancellorElected = true;
			} else if (numberOfYesVotes == breakingPoint) {
				// In this case, the vote of the president breaks the tie.
				var presidentVotedYesOptional = this.voteService.getSingle(x -> x.getRoundId() == currentRoundId && x.getUserId() == currentPresidentId).project(Vote::getVotedChancellor).first();
				if (presidentVotedYesOptional.isEmpty()) {
					return ResponseEntity.badRequest().body(Collections.singletonMap("message", "The president has not voted in the current round yet."));
				}

				if (presidentVotedYesOptional.get()) {
					chancellorElected = true;
				}
			}

			pusher.trigger(channelName, "chancellor_elected", Collections.singletonMap("elected", chancellorElected));

			if (chancellorElected) {
				var policies = new String[]{
						policyModule.getRandomPolicy().getName(), policyModule.getRandomPolicy().getName(), policyModule.getRandomPolicy().getName()
				};

				pusher.trigger(String.format("private-%d", currentPresidentId), "receive_policies", Collections.singletonMap("policies", policies));
			}
		}

		return ResponseEntity.ok(Collections.emptyMap());
	}

}
