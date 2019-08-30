package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.ForeignKeyEntity;
import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;
import de.secrethitler.api.enums.RoleTypes;

/**
 * @author Vladislav Denzel
 */
@TableName("linkedusergamerole")
public class LinkedUserGameRole extends BaseEntity {

	private long userId;
	private long gameId;
	private int roleId;

	@ForeignKeyEntity("roleId")
	private RoleTypes roleType;

	private int sequenceNumber;

	public LinkedUserGameRole(long userId, long gameId, int roleId, int sequenceNumber) {
		this.userId = userId;
		this.gameId = gameId;
		this.roleId = roleId;
		this.sequenceNumber = sequenceNumber;
	}

	public LinkedUserGameRole() {
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getGameId() {
		return gameId;
	}

	public void setGameId(long gameId) {
		this.gameId = gameId;
	}

	public int getRoleId() {
		return roleId;
	}

	public void setRoleId(int roleId) {
		this.roleId = roleId;
	}

	public RoleTypes getRoleType() {
		return roleType;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
}
