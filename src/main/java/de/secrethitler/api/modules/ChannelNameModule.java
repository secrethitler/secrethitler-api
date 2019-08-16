package de.secrethitler.api.modules;

import java.util.Random;

/**
 * @author Collin Alpert
 */
public class ChannelNameModule {

	private static final ChannelNameModule instance;

	static {
		instance = new ChannelNameModule();
	}

	private ChannelNameModule() {
	}

	public static ChannelNameModule getInstance() {
		return instance;
	}

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
