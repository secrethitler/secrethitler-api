package de.secrethitler.api.enums;

import com.github.collinalpert.java2db.contracts.IdentifiableEnum;

/**
 * @author Collin Alpert
 */
public enum RoleTypes implements IdentifiableEnum {

	FASCIST(1), LIBERAL(2), SECRET_HITLER(3);

	private final long id;

	RoleTypes(long id) {
		this.id = id;
	}

	@Override
	public long getId() {
		return this.id;
	}
}
