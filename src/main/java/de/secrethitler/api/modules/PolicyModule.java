package de.secrethitler.api.modules;

import com.github.collinalpert.lambda2sql.functions.SqlFunction;
import de.secrethitler.api.entities.Game;
import de.secrethitler.api.entities.LinkedRoundPolicySuggestion;
import de.secrethitler.api.enums.PolicyTypes;
import de.secrethitler.api.exceptions.EmptyOptionalException;
import de.secrethitler.api.services.GameService;
import de.secrethitler.api.services.LinkedRoundPolicySuggestionService;
import de.secrethitler.api.services.RoundService;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
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
	private final NumberModule numberModule;
	private final RoundService roundService;
	private final LinkedRoundPolicySuggestionService linkedRoundPolicySuggestionService;
	private final LoggingModule logger;

	public PolicyModule(GameService gameService, NumberModule numberModule, RoundService roundService, LinkedRoundPolicySuggestionService linkedRoundPolicySuggestionService, LoggingModule logger) {
		this.gameService = gameService;
		this.numberModule = numberModule;
		this.roundService = roundService;
		this.linkedRoundPolicySuggestionService = linkedRoundPolicySuggestionService;
		this.logger = logger;
	}

	/**
	 * Draws a variable amount of policies from the card deck of a game.
	 *
	 * @param gameId The game to draw policies in.
	 * @param amount The amount of policies to draw.
	 * @return An array containing the drawn policies.
	 * @throws SQLException The exception which can occur when interchanging with the database.
	 */
	public PolicyTypes[] drawPolicies(long gameId, int amount) throws SQLException {
		var game = this.gameService.getById(gameId).orElseThrow(() -> new EmptyOptionalException("No game was found to draw policies from."));

		// In case there are not enough policies left, reintroduce the discarded pile.
		if (game.getAvailableFascistPolicies() + game.getAvailableLiberalPolicies() < amount) {

			game.setAvailableFascistPolicies(game.getAvailableFascistPolicies() + game.getDiscardedFascistPolicies());
			game.setAvailableLiberalPolicies(game.getAvailableLiberalPolicies() + game.getDiscardedLiberalPolicies());
			game.setDiscardedFascistPolicies(0);
			game.setDiscardedLiberalPolicies(0);

			this.gameService.update(game);
		}

		var fascistPolicies = IntStream.range(0, game.getAvailableFascistPolicies()).mapToObj(x -> PolicyTypes.FASCIST);
		var liberalPolicies = IntStream.range(0, game.getAvailableLiberalPolicies()).mapToObj(x -> PolicyTypes.LIBERAL);

		var policies = Stream.concat(fascistPolicies, liberalPolicies).collect(Collectors.toList());
		var randomNumbers = this.numberModule.getUniqueRandomNumbers(policies.size(), amount, game.getCardStackSeed());

		return randomNumbers.stream().map(policies::get).toArray(PolicyTypes[]::new);
	}

	/**
	 * Discards a policy on the database.
	 *
	 * @param policyType The type of policy to discard.
	 * @param gameId     The id of the game to discard the policy in.
	 * @param roundId    The round to discard the policy in.
	 * @throws ExecutionException   The exception which can occur when performing asynchronous operations.
	 * @throws InterruptedException The exception which can occur when performing asynchronous operations.
	 */
	public void discardPolicy(PolicyTypes policyType, long gameId, long roundId) throws ExecutionException, InterruptedException {
		var tasks = new CompletableFuture[2];

		final var discardedPolicyId = policyType.getId();
		var policyLink = this.linkedRoundPolicySuggestionService.getMultiple(x -> x.getRoundId() == roundId && x.getPolicyId() == discardedPolicyId && !x.isDiscarded()).limit(1).first().orElseThrow(() -> new EmptyOptionalException(String.format("Policy '%s' to discard was not found.", policyType.getName())));
		tasks[0] = this.linkedRoundPolicySuggestionService.updateAsync(policyLink.getId(), LinkedRoundPolicySuggestion::isDiscarded, true, logger::log);

		if (policyType == PolicyTypes.FASCIST) {
			tasks[1] = decrementFascistPolicyCountAsync(gameId);
		} else if (policyType == PolicyTypes.LIBERAL) {
			tasks[1] = decrementLiberalPolicyCountAsync(gameId);
		}

		CompletableFuture.allOf(tasks).get();
	}

	/**
	 * An asynchronous operation which represent the decrementation of a fascist policy in the card deck.
	 *
	 * @param gameId The id of the game to update the count of fascist policies in.
	 * @return A task containing the necessary database operations.
	 */
	public CompletableFuture<Void> decrementFascistPolicyCountAsync(long gameId) {
		var availableTask = this.gameService.updateAsync(gameId, availableFascistColumn, (SqlFunction<Game, Integer>) game -> game.getAvailableFascistPolicies() - 1);
		var discardedTask = this.gameService.updateAsync(gameId, discardFascistColumn, (SqlFunction<Game, Integer>) game -> game.getDiscardedFascistPolicies() + 1);

		return CompletableFuture.allOf(availableTask, discardedTask);
	}

	/**
	 * An asynchronous operation which represent the decrementation of a liberal policy in the card deck.
	 *
	 * @param gameId The id of the game to update the count of liberak policies in.
	 * @return A task containing the necessary database operations.
	 */
	public CompletableFuture<Void> decrementLiberalPolicyCountAsync(long gameId) {
		var availableTask = this.gameService.updateAsync(gameId, availableLiberalColumn, (SqlFunction<Game, Integer>) game -> game.getAvailableLiberalPolicies() - 1);
		var discardedTask = this.gameService.updateAsync(gameId, discardLiberalColumn, (SqlFunction<Game, Integer>) game -> game.getDiscardedLiberalPolicies() + 1);

		return CompletableFuture.allOf(availableTask, discardedTask);
	}

	/**
	 * Gets a policy by its name.
	 *
	 * @param policyName The name of the policy.
	 * @return The policy as an enum.
	 */
	public PolicyTypes getByName(String policyName) {
		for (var policy : PolicyTypes.values()) {
			if (policy.getName().toLowerCase().equals(policyName.toLowerCase())) {
				return policy;
			}
		}

		return null;
	}
}
