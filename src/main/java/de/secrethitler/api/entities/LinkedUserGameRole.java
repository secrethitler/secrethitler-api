package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.ForeignKeyEntity;
import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Vladislav Denzel
 */
@TableName("linkedusergamerole")
public class LinkedUserGameRole extends BaseEntity {

	private long userid;
	private long gameid;
    private Integer roleid;

    @ForeignKeyEntity("userid")
    private User user;

    @ForeignKeyEntity("gameid")
    private Game game;

    @ForeignKeyEntity("roleid")
    private Role role;

	public long getUserid() {
        return userid;
    }

	public void setUserid(long userid) {
        this.userid = userid;
    }

	public long getGameid() {
        return gameid;
    }

	public void setGameid(long gameid) {
        this.gameid = gameid;
    }

    public Integer getRoleid() {
        return roleid;
    }

    public void setRoleid(Integer roleid) {
        this.roleid = roleid;
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
