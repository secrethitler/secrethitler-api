package de.secrethitler.api.modules;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author Collin Alpert
 */
@Component
public class RandomNumberModule {

	private static final Random random;

	static {
		random = new Random();
	}

	public Set<Integer> getUniqueRandomNumbers(int maxExclusive, int amount) {
		final var set = new HashSet<Integer>();
		while (set.size() < amount) {
			set.add(random.nextInt(maxExclusive));
		}

		return set;
	}
}
