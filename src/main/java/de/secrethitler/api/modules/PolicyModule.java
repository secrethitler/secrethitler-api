package de.secrethitler.api.modules;

import com.github.collinalpert.lambda2sql.functions.SqlFunction;
import de.secrethitler.api.entities.Game;
import de.secrethitler.api.enums.PolicyTypes;
import de.secrethitler.api.services.GameService;
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

	public PolicyModule(GameService gameService, RandomNumberModule randomNumberModule) {
		this.gameService = gameService;
		this.randomNumberModule = randomNumberModule;
	}

	/**
	 * Draws three policies from the database.
	 *
	 * @param gameId The game to draw policies in
	 * @return A {@code Stream} containing three policies.
	 */
	public Stream<PolicyTypes> drawPolicies(long gameId) throws ExecutionException, InterruptedException {
		var availableFascistTask = this.gameService.getSingle(x -> x.getId() == gameId).project(availableFascistColumn).firstAsync();
		var availableLiberalTask = this.gameService.getSingle(x -> x.getId() == gameId).project(availableLiberalColumn).firstAsync();

		CompletableFuture.allOf(availableFascistTask, availableLiberalTask).get();

		var availableFascistPolicies = availableFascistTask.get().orElse(0);
		var availableLiberalPolicies = availableLiberalTask.get().orElse(0);

		// In case there are not enough policies left, reintroduce the discarded pile.
		if (availableFascistPolicies + availableLiberalPolicies < 3) {
			var discardedFascistTask = this.gameService.getSingle(x -> x.getId() == gameId).project(discardFascistColumn).firstAsync();
			var discardedLiberalTask = this.gameService.getSingle(x -> x.getId() == gameId).project(discardLiberalColumn).firstAsync();

			CompletableFuture.allOf(availableFascistTask, availableLiberalTask).get();

			var discardedFascistPolicies = discardedFascistTask.get().orElse(0);
			var discardedLiberalPolicies = discardedLiberalTask.get().orElse(0);

			this.gameService.updateAsync(gameId, availableFascistColumn, (SqlFunction<Game, Integer>) game -> game.getAvailableFascistPolicies() + discardedFascistPolicies);
			this.gameService.updateAsync(gameId, availableLiberalColumn, (SqlFunction<Game, Integer>) game -> game.getAvailableLiberalPolicies() + discardedLiberalPolicies);
			this.gameService.updateAsync(gameId, discardFascistColumn, 0);
			this.gameService.updateAsync(gameId, discardLiberalColumn, 0);

			availableFascistPolicies += discardedFascistPolicies;
			availableLiberalPolicies += discardedLiberalPolicies;
		}

		var fascistPolicies = IntStream.range(0, availableFascistPolicies).mapToObj(x -> PolicyTypes.FASCIST);
		var liberalPolicies = IntStream.range(0, availableLiberalPolicies).mapToObj(x -> PolicyTypes.LIBERAL);

		var policies = Stream.concat(fascistPolicies, liberalPolicies).collect(Collectors.toList());
		return this.randomNumberModule.getUniqueRandomNumbers(policies.size(), 3).stream().map(policies::get);
	}

	public void discardPolicy(PolicyTypes policyType, long gameId) throws ExecutionException, InterruptedException {
		var tasks = new CompletableFuture[2];

		if (policyType == PolicyTypes.FASCIST) {
			tasks[0] = this.gameService.updateAsync(gameId, availableFascistColumn, (SqlFunction<Game, Integer>) game -> game.getAvailableFascistPolicies() - 1);
			tasks[1] = this.gameService.updateAsync(gameId, discardFascistColumn, (SqlFunction<Game, Integer>) game -> game.getDiscardedFascistPolicies() + 1);
		} else if (policyType == PolicyTypes.LIBERAL) {
			tasks[0] = this.gameService.updateAsync(gameId, availableLiberalColumn, (SqlFunction<Game, Integer>) game -> game.getAvailableLiberalPolicies() - 1);
			tasks[1] = this.gameService.updateAsync(gameId, discardLiberalColumn, (SqlFunction<Game, Integer>) game -> game.getDiscardedLiberalPolicies() + 1);
		}

		CompletableFuture.allOf(tasks).get();
	}
}
