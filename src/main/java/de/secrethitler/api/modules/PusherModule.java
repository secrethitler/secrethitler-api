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

	private PusherModule(PusherConfiguration pusherConfiguration) {
		this.pusherConfiguration = pusherConfiguration;
	}

	public Pusher getPusherInstance() {
		var pusher = new Pusher(pusherConfiguration.getAppId(), pusherConfiguration.getAppKey(), pusherConfiguration.getAppSecret());
		pusher.setCluster(pusherConfiguration.getCluster());

		return pusher;
	}
}