package de.secrethitler.api.modules;

import de.secrethitler.api.entities.Game;
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
}
