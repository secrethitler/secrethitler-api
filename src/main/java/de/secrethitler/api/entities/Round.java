package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.ForeignKeyEntity;
import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Vladislav Denzel
 */
@TableName("round")
public class Round extends BaseEntity {

    private int sequencenumber;
    private long gameid;
    private long presidentid;
    private Long chancellorid;
    private Integer enactedpolicyid;
    private long nominatedchancellorid;

    @ForeignKeyEntity("gameid")
    private Game game;

    @ForeignKeyEntity("enactedpolicyid")
    private Policy policy;

    @ForeignKeyEntity("presidentid")
    private User president;

    @ForeignKeyEntity("chancellorid")
    private User chancellor;

    @ForeignKeyEntity("nominatedchancellorid")
    private User nominatedchancellor;


    public int getSequencenumber() {
        return sequencenumber;
    }

    public void setSequencenumber(int sequencenumber) {
        this.sequencenumber = sequencenumber;
    }

    public long getGameid() {
        return gameid;
    }

    public void setGameid(long gameid) {
        this.gameid = gameid;
    }

    public long getPresidentid() {
        return presidentid;
    }

    public void setPresidentid(long presidentid) {
        this.presidentid = presidentid;
    }

    public Long getChancellorid() {
        return chancellorid;
    }

    public void setChancellorid(Long chancellorid) {
        this.chancellorid = chancellorid;
    }

    public Integer getEnactedpolicyid() {
        return enactedpolicyid;
    }

    public void setEnactedpolicyid(Integer enactedpolicyid) {
        this.enactedpolicyid = enactedpolicyid;
    }

    public long getNominatedchancellorid() {
        return nominatedchancellorid;
    }

    public void setNominatedchancellorid(long nominatedchancellorid) {
        this.nominatedchancellorid = nominatedchancellorid;
    }

    public Game getGame() {
        return game;
    }

    public Policy getPolicy() {
        return policy;
    }

    public User getPresident() {
        return president;
    }

    public User getChancellor() {
        return chancellor;
    }

    public User getNominatedchancellor() {
        return nominatedchancellor;
    }

}
