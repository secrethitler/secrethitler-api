package de.secrethitler.api.modules;

import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * @author Collin Alpert
 */
@Component
public class ChannelNameModule {

	/**
	 * Creates a random, six-digit String which serves as the channelName for a game.
	 *
	 * @return A String representing a channelName.
	 */
	public String generateChannelName() {
		var random1 = new Random().nextInt(10);
		var random2 = new Random().nextInt(10);
		var random3 = new Random().nextInt(10);
		var random4 = new Random().nextInt(10);
		var random5 = new Random().nextInt(10);
		var random6 = new Random().nextInt(10);

		return Integer.toString(random1) + random2 + random3 + random4 + random5 + random6;
	}
}
