package de.secrethitler.api.modules;

import com.pusher.rest.Pusher;
import de.secrethitler.api.config.PusherConfiguration;
import org.springframework.stereotype.Component;

/**
 * @author Collin Alpert
 */
@Component
public class PusherModule {

	private final PusherConfiguration pusherConfiguration;
	private Pusher pusher;

	private PusherModule(PusherConfiguration pusherConfiguration) {
		this.pusherConfiguration = pusherConfiguration;
	}

	public Pusher getPusherInstance() {
		if (this.pusher != null) {
			return this.pusher;
		}

		return this.pusher = generatePusherInstance();
	}

	private Pusher generatePusherInstance() {
		var pusher = new Pusher(pusherConfiguration.getAppId(), pusherConfiguration.getAppKey(), pusherConfiguration.getAppSecret());
		pusher.setCluster(pusherConfiguration.getCluster());

		return pusher;
	}
}