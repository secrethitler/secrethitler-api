package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.ForeignKeyEntity;
import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;
import de.secrethitler.api.enums.RoleTypes;

/**
 * @author Vladislav Denzel
 */
@TableName("linkedUserGameRole")
public class LinkedUserGameRole extends BaseEntity {

	private String userName;
	private long gameId;
	private Long roleId;

	@ForeignKeyEntity("roleId")
	private RoleTypes role;

	private int sequenceNumber;

	public LinkedUserGameRole(String userName) {
		this.userName = userName;
	}

	public LinkedUserGameRole() {
	}

	public String getUserName() {
		return userName;
	}

	public long getGameId() {
		return gameId;
	}

	public Long getRoleId() {
		return roleId;
	}

	public RoleTypes getRole() {
		return role;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}
}
