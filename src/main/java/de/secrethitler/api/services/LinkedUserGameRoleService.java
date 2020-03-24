package de.secrethitler.api.services;

import com.github.collinalpert.java2db.services.AsyncBaseService;
import de.secrethitler.api.entities.LinkedUserGameRole;
import de.secrethitler.api.modules.HeaderModule;
import org.springframework.stereotype.Service;

/**
 * @author Vladislav Denzel
 */
@Service
public class LinkedUserGameRoleService extends AsyncBaseService<LinkedUserGameRole> {

	private final HeaderModule headerModule;

	public LinkedUserGameRoleService(HeaderModule headerModule) {
		this.headerModule = headerModule;
	}

	public boolean hasValidToken(long id, String base64Token) {
		var token = this.headerModule.getAuthorizationToken(base64Token);

		return any(x -> x.getId() == id && x.getToken() == token && !x.isExecuted());
	}
}
