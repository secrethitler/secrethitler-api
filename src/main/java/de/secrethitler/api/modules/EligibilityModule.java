package de.secrethitler.api.modules;

import de.secrethitler.api.entities.Game;
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

	public boolean isPolicyPeekEligible(Game game) {
		var gameId = game.getId();
		var fascistId = PolicyTypes.FASCIST.getId();

		return game.getInitialPlayerCount() >= 5 &&
				game.getInitialPlayerCount() <= 6 &&
				this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == fascistId) == 3;
	}

	public boolean isPlayerExecutionEligible(long gameId) {
		var fascistId = PolicyTypes.FASCIST.getId();
		var enactedFascistPolicies = this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == fascistId);

		return enactedFascistPolicies == 4 || enactedFascistPolicies == 5;
	}

	public boolean isLoyaltyInvestigationEligible(Game game) {
		var fascistId = PolicyTypes.FASCIST.getId();
		var gameId = game.getId();
		var enactedFascistPolicies = this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == fascistId);

		return (game.getInitialPlayerCount() >= 7 && game.getInitialPlayerCount() <= 8 && enactedFascistPolicies == 2) || (game.getInitialPlayerCount() >= 9 && game.getInitialPlayerCount() <= 10 && (enactedFascistPolicies == 1 || enactedFascistPolicies == 2));
	}

	public boolean isSpecialElectionEligible(Game game) {
		var fascistId = PolicyTypes.FASCIST.getId();
		var gameId = game.getId();
		var enactedFascistPolicies = this.roundService.count(x -> x.getGameId() == gameId && x.getEnactedPolicyId() == fascistId);

		return game.getInitialPlayerCount() >= 7 && game.getInitialPlayerCount() <= 10 && enactedFascistPolicies == 3;
	}
}
