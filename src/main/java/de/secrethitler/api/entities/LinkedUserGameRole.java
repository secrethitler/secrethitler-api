package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.ForeignKeyEntity;
import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Vladislav Denzel
 */
@TableName("linkedusergamerole")
public class LinkedUserGameRole extends BaseEntity {

	private long userId;
	private long gameId;
	private Integer roleId;

	@ForeignKeyEntity("userId")
    private User user;

	@ForeignKeyEntity("gameId")
    private Game game;

	@ForeignKeyEntity("roleId")
    private Role role;

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

	public Integer getRoleId() {
		return roleId;
    }

	public void setRoleId(Integer roleId) {
		this.roleId = roleId;
    }

    public User getUser() {
        return user;
    }

    public Game getGame() {
        return game;
    }

    public Role getRole() {
        return role;
    }


}
