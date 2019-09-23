package de.secrethitler.api.modules;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author Collin Alpert
 */
@Component
public class NumberModule {

	private static final Random random;

	static {
		random = new Random();
	}

	public Set<Integer> getUniqueRandomNumbers(int maxExclusive, int amount, int seed) {
		random.setSeed(seed);

		final var set = new HashSet<Integer>();
		while (set.size() < amount) {
			set.add(random.nextInt(maxExclusive));
		}

		return set;
	}

	public long getAsLong(Object object) {
		if (object instanceof Integer) {
			return (int) object;
		} else if (object instanceof Long) {
			return (long) object;
		} else {
			throw new ClassCastException();
		}
	}
}
