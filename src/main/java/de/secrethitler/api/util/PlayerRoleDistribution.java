package de.secrethitler.api.util;

import de.secrethitler.api.enums.RoleTypes;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Collin Alpert
 */
public class PlayerRoleDistribution {

	private final FixedCapacityStack<RoleTypes> roleTypes;

	public PlayerRoleDistribution(int numberOfPlayers) {
		this.roleTypes = new FixedCapacityStack<>(numberOfPlayers);
		populateList(numberOfPlayers);
	}

	private void populateList(int numberOfPlayers) {
		var tempRoles = new ArrayList<RoleTypes>();

		tempRoles.add(RoleTypes.SECRET_HITLER);

		switch (numberOfPlayers) {
			case 5:
			case 6: {
				tempRoles.add(RoleTypes.FASCIST);

				for (int i = 1; i < numberOfPlayers - 1; i++) {
					tempRoles.add(RoleTypes.LIBERAL);
				}

				break;
			}
			case 7:
			case 8: {
				tempRoles.add(RoleTypes.FASCIST);
				tempRoles.add(RoleTypes.FASCIST);

				for (int i = 1; i < numberOfPlayers - 2; i++) {
					tempRoles.add(RoleTypes.LIBERAL);
				}

				break;
			}
			case 9:
			case 10: {
				tempRoles.add(RoleTypes.FASCIST);
				tempRoles.add(RoleTypes.FASCIST);
				tempRoles.add(RoleTypes.FASCIST);

				for (int i = 1; i < numberOfPlayers - 3; i++) {
					tempRoles.add(RoleTypes.LIBERAL);
				}

				break;
			}
		}

		Collections.shuffle(tempRoles);

		tempRoles.forEach(this.roleTypes::push);
	}

	public RoleTypes getNextRole() {
		return this.roleTypes.pop();
	}
}
