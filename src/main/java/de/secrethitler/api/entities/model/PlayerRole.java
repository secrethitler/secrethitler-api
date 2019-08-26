package de.secrethitler.api.entities.model;

import java.util.List;

/**
 * @author Collin Alpert
 */
public class PlayerRole {

	private long userId;
	private long roleId;
	private String roleName;
	private String userName;

	private List<PlayerRole> partyMembers;

	public PlayerRole(long userId, long roleId, String roleName, String userName) {
		this.userId = userId;
		this.roleId = roleId;
		this.roleName = roleName;
		this.userName = userName;
	}

	public PlayerRole(PlayerRole instance) {
		this.userId = instance.getUserId();
		this.roleId = instance.getRoleId();
		this.roleName = instance.getRoleName();
		this.userName = instance.getUserName();
		this.partyMembers = instance.getPartyMembers();
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public List<PlayerRole> getPartyMembers() {
		return partyMembers;
	}

	public void setPartyMembers(List<PlayerRole> partyMembers) {
		this.partyMembers = partyMembers;
	}
}
