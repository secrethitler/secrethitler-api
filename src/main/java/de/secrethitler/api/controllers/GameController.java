package de.secrethitler.api.controllers;

import de.secrethitler.api.services.PolicyService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/game")
public class GameController {

	private final String userName = "user_name";

	@PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> createGame(@RequestBody Map<String, String> requestBody, HttpSession session) {
		new PolicyService().getAll();
		return ResponseEntity.ok("Hello");
	}

	@PostMapping(value = "/join", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> joinGame(@RequestBody Map<String, String> requestBody, HttpSession session) {
		return ResponseEntity.ok("Hello");
	}
}
