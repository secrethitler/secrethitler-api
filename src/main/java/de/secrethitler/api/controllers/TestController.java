package de.secrethitler.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * @author Collin Alpert
 */
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = {"http://10.14.208.75", "http://localhost", "http://localhost:8080", "https://secret-hitler.netlify.com", "https://geheimerdeutscher.tk"}, allowCredentials = "true")
public class TestController {

	@GetMapping
	public ResponseEntity<Map<String, Object>> testGet() {
		return ResponseEntity.ok(Collections.singletonMap("message", "Hi!"));
	}
}
