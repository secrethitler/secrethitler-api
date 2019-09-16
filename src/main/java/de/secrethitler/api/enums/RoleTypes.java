package de.secrethitler.api.enums;

import com.github.collinalpert.java2db.contracts.IdentifiableEnum;

/**
 * @author Collin Alpert
 */
public enum RoleTypes implements IdentifiableEnum {

	FASCIST(1, "fascist"), LIBERAL(2, "liberal"), SECRET_HITLER(3, "secret hitler");

	private final long id;
	private final String name;

	RoleTypes(long id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public long getId() {
		return this.id;
	}

	public String getName() {
		return name;
	}
}
