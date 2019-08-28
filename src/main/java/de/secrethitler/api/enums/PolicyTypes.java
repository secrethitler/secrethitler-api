package de.secrethitler.api.enums;

import com.github.collinalpert.java2db.contracts.IdentifiableEnum;

/**
 * @author Collin Alpert
 */
public enum PolicyTypes implements IdentifiableEnum {

	FASCIST(1), LIBERAL(2);

	private final long id;

	PolicyTypes(long id) {
		this.id = id;
	}

	@Override
	public long getId() {
		return this.id;
	}
}
