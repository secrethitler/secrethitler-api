package de.secrethitler.api.modules;

import de.secrethitler.api.enums.PolicyTypes;
import org.springframework.stereotype.Component;

/**
 * @author Collin Alpert
 */
@Component
public class PolicyModule {

	private static final double fascistWeight = 11d / 17d;

	public PolicyTypes getRandomPolicy() {
		if (Math.random() < fascistWeight) {
			return PolicyTypes.FASCIST;
		}

		return PolicyTypes.LIBERAL;
	}
}
