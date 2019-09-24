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

	/**
	 * Generates a set of unique and random numbers.
	 *
	 * @param maxExclusive The maximum value of the random numbers, exclusively.
	 * @param amount       The amount of numbers to generate.
	 * @param seed         The seed to use for the random generator.
	 * @return A set of unique and random numbers.
	 */
	public Set<Integer> getUniqueRandomNumbers(int maxExclusive, int amount, int seed) {
		random.setSeed(seed);

		final var set = new HashSet<Integer>();
		while (set.size() < amount) {
			set.add(random.nextInt(maxExclusive));
		}

		return set;
	}

	/**
	 * Converts an object to a long, if possible.
	 *
	 * @param object The object to convert.
	 * @return The object in its {@link Long} representation or a {@link ClassCastException} if the conversion fails.
	 */
	public long getAsLong(Object object) {
		if (object instanceof Integer) {
			return (int) object;
		} else if (object instanceof Long) {
			return (long) object;
		} else if (object instanceof String) {
			return Long.parseLong((String) object);
		} else {
			throw new ClassCastException();
		}
	}
}
