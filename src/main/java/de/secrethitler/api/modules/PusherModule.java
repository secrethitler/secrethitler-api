package de.secrethitler.api.modules;

import com.pusher.rest.Pusher;
import de.secrethitler.api.config.PusherConfiguration;

/**
 * @author Collin Alpert
 */
public class PusherModule {

	private static final PusherModule instance;

	static {
		instance = new PusherModule(new PusherConfiguration());
	}

	private final PusherConfiguration pusherConfiguration;

	private PusherModule(PusherConfiguration pusherConfiguration) {
		this.pusherConfiguration = pusherConfiguration;
	}

	public static PusherModule getInstance() {
		return instance;
	}

	public Pusher getPusher() {
		var pusher = new Pusher(pusherConfiguration.getAppId(), pusherConfiguration.getAppKey(), pusherConfiguration.getAppSecret());
		pusher.setCluster(pusherConfiguration.getCluster());

		return pusher;
	}
}