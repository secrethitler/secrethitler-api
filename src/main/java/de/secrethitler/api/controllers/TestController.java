package de.secrethitler.api.controllers;

import com.pusher.rest.Pusher;
import de.secrethitler.api.config.PusherConfiguration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api")
public class TestController {

	private final PusherConfiguration pusherConfiguration;

	public TestController(PusherConfiguration pusherConfiguration) {
		this.pusherConfiguration = pusherConfiguration;
	}

	@GetMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
	public void test() {
		var pusher = new Pusher(pusherConfiguration.getAppId(), pusherConfiguration.getAppKey(), pusherConfiguration.getAppSecret());
		pusher.setCluster(pusherConfiguration.getCluster());

		pusher.trigger("channel-one", "test_event", Map.of("message", "hello world", "status", "failed"));
	}
}
