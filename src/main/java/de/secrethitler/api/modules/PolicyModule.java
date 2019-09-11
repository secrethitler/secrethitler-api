package de.secrethitler.api.modules;

import com.github.collinalpert.java2db.queries.OrderTypes;
import com.github.collinalpert.lambda2sql.functions.SqlFunction;
import de.secrethitler.api.entities.Game;
import de.secrethitler.api.entities.LinkedRoundPolicySuggestion;
import de.secrethitler.api.entities.Round;
import de.secrethitler.api.enums.PolicyTypes;
import de.secrethitler.api.exceptions.EmptyOptionalException;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.LinkedRoundPolicySuggestionService;
import de.secrethitler.api.services.RoundService;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Collin Alpert
 */
@Component
public class PolicyModule {

	private static final SqlFunction<Game, Integer> availableFascistColumn = Game::getAvailableFascistPolicies;
	private static final SqlFunction<Game, Integer> availableLiberalColumn = Game::getAvailableLiberalPolicies;
	private static final SqlFunction<Game, Integer> discardFascistColumn = Game::getDiscardedFascistPolicies;
	private static final SqlFunction<Game, Integer> discardLiberalColumn = Game::getDiscardedLiberalPolicies;

	private final GameService gameService;
	private final RandomNumberModule randomNumberModule;
	private final RoundService roundService;
	private final LinkedRoundPolicySuggestionService linkedRoundPolicySuggestionService;
	private final LoggingModule logger;

	public PolicyModule(GameService gameService, RandomNumberModule randomNumberModule, RoundService roundService, LinkedRoundPolicySuggestionService linkedRoundPolicySuggestionService, LoggingModule logger) {
		this.gameService = gameService;
		this.randomNumberModule = randomNumberModule;
		this.roundService = roundService;
		this.linkedRoundPolicySuggestionService = linkedRoundPolicySuggestionService;
		this.logger = logger;
	}

	/**
	 * Draws three policies from the database.
	 *
	 * @param gameId The game to draw policies in.
	 * @return An array containing three policies.
	 */
	public PolicyTypes[] drawPolicies(long gameId, int amount) throws ExecutionException, InterruptedException {
		var availableFascistTask = this.gameService.getSingle(x -> x.getId() == gameId).project(availableFascistColumn).firstAsync();
		var availableLiberalTask = this.gameService.getSingle(x -> x.getId() == gameId).project(availableLiberalColumn).firstAsync();
		var cardStackSeedTask = this.gameService.getSingle(x -> x.getId() == gameId).project(Game::getCardStackSeed).firstAsync();

		CompletableFuture.allOf(availableFascistTask, availableLiberalTask, cardStackSeedTask).get();

		var availableFascistPolicies = availableFascistTask.get().orElseThrow(() -> new EmptyOptionalException(String.format("No available fascist policies were found for game id %d.", gameId)));
		var availableLiberalPolicies = availableLiberalTask.get().orElseThrow(() -> new EmptyOptionalException(String.format("No available liberal policies were found for game id %d.", gameId)));
		var cardStackSeed = availableLiberalTask.get().orElseThrow(() -> new EmptyOptionalException(String.format("No available liberal policies were found for game id %d.", gameId)));

		// In case there are not enough policies left, reintroduce the discarded pile.
		if (availableFascistPolicies + availableLiberalPolicies < amount) {
			var discardedFascistTask = this.gameService.getSingle(x -> x.getId() == gameId).project(discardFascistColumn).firstAsync();
			var discardedLiberalTask = this.gameService.getSingle(x -> x.getId() == gameId).project(discardLiberalColumn).firstAsync();

			CompletableFuture.allOf(availableFascistTask, availableLiberalTask).get();

			var discardedFascistPolicies = discardedFascistTask.get().orElse(0);
			var discardedLiberalPolicies = discardedLiberalTask.get().orElse(0);

			this.gameService.updateAsync(gameId, availableFascistColumn, (SqlFunction<Game, Integer>) game -> game.getAvailableFascistPolicies() + discardedFascistPolicies, logger::log);
			this.gameService.updateAsync(gameId, availableLiberalColumn, (SqlFunction<Game, Integer>) game -> game.getAvailableLiberalPolicies() + discardedLiberalPolicies, logger::log);
			this.gameService.updateAsync(gameId, discardFascistColumn, 0, logger::log);
			this.gameService.updateAsync(gameId, discardLiberalColumn, 0, logger::log);

			availableFascistPolicies += discardedFascistPolicies;
			availableLiberalPolicies += discardedLiberalPolicies;
		}

		var fascistPolicies = IntStream.range(0, availableFascistPolicies).mapToObj(x -> PolicyTypes.FASCIST);
		var liberalPolicies = IntStream.range(0, availableLiberalPolicies).mapToObj(x -> PolicyTypes.LIBERAL);

		var policies = Stream.concat(fascistPolicies, liberalPolicies).collect(Collectors.toList());
		return this.randomNumberModule.getUniqueRandomNumbers(policies.size(), amount, cardStackSeed).stream().map(policies::get).toArray(PolicyTypes[]::new);
	}

	public void discardPolicy(PolicyTypes policyType, long gameId, Long roundId) throws ExecutionException, InterruptedException {
		if (roundId == null) {
			roundId = this.roundService.getMultiple(x -> x.getGameId() == gameId).orderBy(OrderTypes.DESCENDING, Round::getSequenceNumber).limit(1).project(Round::getId).first().orElseThrow(() -> new EmptyOptionalException("No round was found in the current game."));
		}

		var tasks = new CompletableFuture[2];

		final var currentRoundId = roundId;
		final var discardedPolicyId = policyType.getId();
		var policyLink = this.linkedRoundPolicySuggestionService.getMultiple(x -> x.getRoundId() == currentRoundId && x.getPolicyId() == discardedPolicyId && !x.isDiscarded()).limit(1).first().orElseThrow(() -> new EmptyOptionalException(String.format("Policy '%s' to discard was not found.", policyType.getName())));
		tasks[0] = this.linkedRoundPolicySuggestionService.updateAsync(policyLink.getId(), LinkedRoundPolicySuggestion::isDiscarded, true, logger::log);

		if (policyType == PolicyTypes.FASCIST) {
			tasks[1] = decrementFascistPolicyCountAsync(gameId);
		} else if (policyType == PolicyTypes.LIBERAL) {
			tasks[1] = decrementLiberalPolicyCountAsync(gameId);
		}

		CompletableFuture.allOf(tasks).get();
	}

	public CompletableFuture<Void> decrementFascistPolicyCountAsync(long gameId) {
		var availableTask = this.gameService.updateAsync(gameId, availableFascistColumn, (SqlFunction<Game, Integer>) game -> game.getAvailableFascistPolicies() - 1);
		var discardedTask = this.gameService.updateAsync(gameId, discardFascistColumn, (SqlFunction<Game, Integer>) game -> game.getDiscardedFascistPolicies() + 1);

		return CompletableFuture.allOf(availableTask, discardedTask);
	}

	public CompletableFuture<Void> decrementLiberalPolicyCountAsync(long gameId) {
		var availableTask = this.gameService.updateAsync(gameId, availableLiberalColumn, (SqlFunction<Game, Integer>) game -> game.getAvailableLiberalPolicies() - 1);
		var discardedTask = this.gameService.updateAsync(gameId, discardLiberalColumn, (SqlFunction<Game, Integer>) game -> game.getDiscardedLiberalPolicies() + 1);

		return CompletableFuture.allOf(availableTask, discardedTask);
	}

	public PolicyTypes getByName(String policyName) {
		for (var policy : PolicyTypes.values()) {
			if (policy.getName().toLowerCase().equals(policyName.toLowerCase())) {
				return policy;
			}
		}

		return null;
	}
}
