package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.ForeignKeyEntity;
import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Vladislav Denzel
 */
@TableName("linkedusergamerole")
public class LinkedUserGameRole extends BaseEntity {

    private int userid;
    private int gameid;
    private Integer roleid;

    @ForeignKeyEntity("userid")
    private User user;

    @ForeignKeyEntity("gameid")
    private Game game;

    @ForeignKeyEntity("roleid")
    private Role role;

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getGameid() {
        return gameid;
    }

    public void setGameid(int gameid) {
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
