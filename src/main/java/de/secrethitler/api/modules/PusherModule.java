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

	/**
	 * Gets the singleton instance of the Pusher API to send events with.
	 *
	 * @return The Pusher instance.
	 */
	public Pusher getPusherInstance() {
		return Objects.requireNonNullElse(this.pusher, this.pusher = generatePusherInstance());
	}

	/**
	 * Triggers a Pusher event using the singleton Pusher instance.
	 *
	 * @param channelName The channel to trigger the event on.
	 * @param eventName   The name of the event name.
	 * @param data        The data so send with the event.
	 */
	public void trigger(String channelName, String eventName, Object data) {
		getPusherInstance().trigger(channelName, eventName, data);
	}

	/**
	 * Generates a new Pusher instance. This should only happen once.
	 *
	 * @return A new Pusher instance.
	 */
	private Pusher generatePusherInstance() {
		var pusher = new Pusher(pusherConfiguration.getAppId(), pusherConfiguration.getAppKey(), pusherConfiguration.getAppSecret());
		pusher.setCluster(pusherConfiguration.getCluster());

		return pusher;
	}
}