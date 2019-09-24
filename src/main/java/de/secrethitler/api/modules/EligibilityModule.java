package de.secrethitler.api.modules;

import de.secrethitler.api.entities.Game;
import de.secrethitler.api.entities.Round;
import de.secrethitler.api.enums.ExecutiveActionTypes;
import de.secrethitler.api.enums.PolicyTypes;
import de.secrethitler.api.services.RoundService;
import org.springframework.stereotype.Component;

/**
 * @author Collin Alpert
 */
@Component
public class EligibilityModule {

	private final RoundService roundService;

	public EligibilityModule(RoundService roundService) {
		this.roundService = roundService;
	}

	/**
	 * Scans a game for a possible executive action which might be necessary.
	 *
	 * @param game The game to check an executive action for.
	 * @return An executive action, if necessary. This method will return {@code null} if none is in order.
	 */
	public ExecutiveActionTypes getExecutiveAction(Game game) {
		var gameId = game.getId();
		var fascistId = PolicyTypes.FASCIST.getId();
		var enactedFascistPolicies = this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == fascistId);

		if (game.getInitialPlayerCount() >= 5 &&
				game.getInitialPlayerCount() <= 6 && enactedFascistPolicies == 3) {
			return ExecutiveActionTypes.POLICY_PEEK;
		}

		if (enactedFascistPolicies >= 4 && enactedFascistPolicies <= 5) {
			return ExecutiveActionTypes.EXECUTE_PLAYER;
		}

		if ((game.getInitialPlayerCount() >= 7 && game.getInitialPlayerCount() <= 8 && enactedFascistPolicies == 2) ||
				(game.getInitialPlayerCount() >= 9 && game.getInitialPlayerCount() <= 10 && (enactedFascistPolicies == 1 || enactedFascistPolicies == 2))) {
			return ExecutiveActionTypes.LOYALTY_INVESTIGATION;
		}

		if (game.getInitialPlayerCount() >= 7 && game.getInitialPlayerCount() <= 10 && enactedFascistPolicies == 3) {
			return ExecutiveActionTypes.SPECIAL_ELECTION;
		}

		return null;
	}

	/**
	 * Checks if policy peek is possible in the current state of the game.
	 *
	 * @param game The game to check in.
	 * @return {@code True} if policy peek is currently allowed, {@code false} if not.
	 */
	public boolean isPolicyPeekEligible(Game game) {
		var gameId = game.getId();
		var fascistId = PolicyTypes.FASCIST.getId();

		return game.getInitialPlayerCount() >= 5 &&
				game.getInitialPlayerCount() <= 6 &&
				this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == fascistId) == 3;
	}

	/**
	 * Checks if player execution is possible in the current state of the game.
	 *
	 * @param gameId The id of the game to check in
	 * @return {@code True} if player execution is currently allowed, {@code false} if not.
	 */
	public boolean isPlayerExecutionEligible(long gameId) {
		var fascistId = PolicyTypes.FASCIST.getId();
		var enactedFascistPolicies = this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == fascistId);

		return enactedFascistPolicies == 4 || enactedFascistPolicies == 5;
	}

	/**
	 * Checks if loyalty investigation is possible in the current state of the game.
	 *
	 * @param game The game to check in.
	 * @return {@code True} if loyalty investigation is currently allowed, {@code false} if not.
	 */
	public boolean isLoyaltyInvestigationEligible(Game game) {
		var fascistId = PolicyTypes.FASCIST.getId();
		var gameId = game.getId();
		var enactedFascistPolicies = this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == fascistId);

		return (game.getInitialPlayerCount() >= 7 && game.getInitialPlayerCount() <= 8 && enactedFascistPolicies == 2) || (game.getInitialPlayerCount() >= 9 && game.getInitialPlayerCount() <= 10 && (enactedFascistPolicies == 1 || enactedFascistPolicies == 2));
	}

	/**
	 * Checks if a special election is possible in the current state of the game.
	 *
	 * @param game The game to check in.
	 * @return {@code True} if loyalty investigation is currently allowed, {@code false} if not.
	 */
	public boolean isSpecialElectionEligible(Game game) {
		var fascistId = PolicyTypes.FASCIST.getId();
		var gameId = game.getId();
		var enactedFascistPolicies = this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == fascistId);

		return game.getInitialPlayerCount() >= 7 && game.getInitialPlayerCount() <= 10 && enactedFascistPolicies == 3;
	}

	/**
	 * Checks if a user can be chancellor based on his role in the previous round.
	 *
	 * @param previousRound The previous round.
	 * @param chancellorId  The id of the user whom shall be checked for eligibility of the chancellor office.
	 * @return {@code True} if he is eligible, {@code false} if not.
	 */
	public boolean isChancellorEligible(Round previousRound, long chancellorId) {
		return previousRound.getChancellorId() != null && (previousRound.getChancellorId() == chancellorId || previousRound.getPresidentId() == chancellorId);
	}
}
