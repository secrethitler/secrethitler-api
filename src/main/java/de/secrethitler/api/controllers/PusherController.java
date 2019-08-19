package de.secrethitler.api.controllers;

import de.secrethitler.api.modules.PusherModule;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/pusher")
public class PusherController {

	private final String socketIdParameter = "socket_id";
	private final String channelNameParameter = "channel_name";

	@CrossOrigin
	@PostMapping(value = "/auth/presence", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> authenticate(@RequestBody Map<String, Object> requestBody) {
		var pusher = PusherModule.getInstance().getPusher();
		var socketId = (String) requestBody.get(this.socketIdParameter);
		var channelName = (String) requestBody.get(this.channelNameParameter);
		pusher.authenticate(socketId, "presence-" + channelName);

		return ResponseEntity.ok(Collections.emptyMap());
	}
}