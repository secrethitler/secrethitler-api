package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.ForeignKeyEntity;
import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Vladislav Denzel
 */
@TableName("linkedusergamerole")
public class Vote extends BaseEntity {

    private int userid;
    private int roundid;
    private boolean votedforchancellor;

    @ForeignKeyEntity("userid")
    private User user;

    @ForeignKeyEntity("roundid")
    private Round round;


    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getRoundid() {
        return roundid;
    }

    public void setRoundid(int roundid) {
        this.roundid = roundid;
    }

    public boolean getVotedforchancellor() {
        return votedforchancellor;
    }

    public void setVotedforchancellor(boolean votedforchancellor) {
        this.votedforchancellor = votedforchancellor;
    }

    public User getUser() {
        return user;
    }

    public Round getRound() {
        return round;
    }

}
