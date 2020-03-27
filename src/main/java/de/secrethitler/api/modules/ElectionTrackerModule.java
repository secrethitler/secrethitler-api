package de.secrethitler.api.modules;

import com.github.collinalpert.lambda2sql.functions.SqlFunction;
import de.secrethitler.api.entities.Game;
import de.secrethitler.api.entities.Round;
import de.secrethitler.api.enums.PolicyTypes;
import de.secrethitler.api.enums.RoleTypes;
import de.secrethitler.api.exceptions.EmptyOptionalException;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.RoundService;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Modules which handles everything which has to do with the election tracker.
 *
 * @author Collin Alpert
 */
@Component
public class ElectionTrackerModule {

	private final GameService gameService;
	private final RoundService roundService;
	private final PolicyModule policyModule;
	private final PusherModule pusherModule;
	private final LoggingModule logger;

	public ElectionTrackerModule(GameService gameService, RoundService roundService, PolicyModule policyModule, PusherModule pusherModule, LoggingModule loggingModule) {
		this.gameService = gameService;
		this.roundService = roundService;
		this.policyModule = policyModule;
		this.pusherModule = pusherModule;
		this.logger = loggingModule;
	}

	public void enactPolicyIfNecessary(String channelName, long gameId, long currentRoundId) throws SQLException, ExecutionException, InterruptedException {
		int electionTrackings = this.gameService.getSingle(x -> x.getId() == gameId).project(Game::getElectionTrackings).first().orElseThrow(() -> new EmptyOptionalException("No game found for election tracking."));
		var failedRounds = this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == null);
		if (failedRounds >= (electionTrackings * 3) - electionTrackings + 3) {
			var policyToEnact = this.policyModule.drawPolicies(gameId, 1)[0];
			var roundTask = this.roundService.updateAsync(currentRoundId, Round::getEnactedPolicyId, policyToEnact.getId(), logger::log);
			var gameTask = this.gameService.updateAsync(gameId, (SqlFunction<Game, Integer>) Game::getElectionTrackings, (SqlFunction<Game, Integer>) game -> game.getElectionTrackings() + 1, logger::log);

			this.pusherModule.getPusherInstance().trigger(channelName, "electionTracker", Collections.emptyMap());
			this.pusherModule.getPusherInstance().trigger(channelName, "policyEnacted", Collections.singletonMap("policy", policyToEnact.getName()));

			CompletableFuture.allOf(roundTask, gameTask).get();

			if (policyToEnact == PolicyTypes.FASCIST) {
				var fascistPolicyId = PolicyTypes.FASCIST.getId();
				this.roundService.countAsync(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == fascistPolicyId, policyCount -> winByFascistPolicy(channelName, policyCount));
			} else if (policyToEnact == PolicyTypes.LIBERAL) {
				var liberalPolicyId = PolicyTypes.LIBERAL.getId();
				this.roundService.countAsync(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == liberalPolicyId, policyCount -> winByLiberalPolicy(channelName, policyCount));
			}
		}
	}

	/**
	 * Checks if the Liberals have won by policy count.
	 *
	 * @param channelName The channelName to trigger the event to.
	 * @param policyCount The number of enacted liberal policies.
	 */
	private void winByLiberalPolicy(String channelName, Long policyCount) {
		if (policyCount >= 5) {
			this.pusherModule.trigger(channelName, "gameWon", Map.of("party", RoleTypes.LIBERAL.getName(), "reason", "The Liberals enacted five liberal policies!"));
		}
	}

	/**
	 * Checks if the Fascists have won by policy count.
	 *
	 * @param channelName The channelName to trigger the event to.
	 * @param policyCount The number of enacted fascist policies.
	 */
	private void winByFascistPolicy(String channelName, Long policyCount) {
		if (policyCount >= 6) {
			this.pusherModule.trigger(channelName, "gameWon", Map.of("party", RoleTypes.FASCIST.getName(), "reason", "The Fascists enacted six fascist policies!"));
		}
	}
}
