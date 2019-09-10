package de.secrethitler.api.modules;

import com.pusher.rest.Pusher;
import de.secrethitler.api.config.PusherConfiguration;
import org.springframework.stereotype.Component;

import java.util.Objects;

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
		return Objects.requireNonNullElse(this.pusher, this.pusher = generatePusherInstance());
	}

	public void trigger(String channelName, String eventName, Object data) {
		Objects.requireNonNullElse(this.pusher, this.pusher = generatePusherInstance()).trigger(channelName, eventName, data);
	}

	private Pusher generatePusherInstance() {
		var pusher = new Pusher(pusherConfiguration.getAppId(), pusherConfiguration.getAppKey(), pusherConfiguration.getAppSecret());
		pusher.setCluster(pusherConfiguration.getCluster());

		return pusher;
	}
}