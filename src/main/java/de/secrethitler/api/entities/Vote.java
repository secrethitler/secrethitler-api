package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.ForeignKeyEntity;
import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Vladislav Denzel
 */
@TableName("linkedusergamerole")
public class Vote extends BaseEntity {

    private long userid;
    private long roundid;
    private boolean votedforchancellor;

    @ForeignKeyEntity("userid")
    private User user;

    @ForeignKeyEntity("roundid")
    private Round round;


    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
        this.userid = userid;
    }

    public long getRoundid() {
        return roundid;
    }

    public void setRoundid(long roundid) {
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
