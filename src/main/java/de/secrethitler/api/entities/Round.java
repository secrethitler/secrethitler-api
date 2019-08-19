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
    private int gameid;
    private int presidentid;
    private Integer chancellorid;
    private Integer enactedpolicyid;
    private int nominatedchancellorid;

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

    public int getGameid() {
        return gameid;
    }

    public void setGameid(int gameid) {
        this.gameid = gameid;
    }

    public int getPresidentid() {
        return presidentid;
    }

    public void setPresidentid(int presidentid) {
        this.presidentid = presidentid;
    }

    public Integer getChancellorid() {
        return chancellorid;
    }

    public void setChancellorid(Integer chancellorid) {
        this.chancellorid = chancellorid;
    }

    public Integer getEnactedpolicyid() {
        return enactedpolicyid;
    }

    public void setEnactedpolicyid(Integer enactedpolicyid) {
        this.enactedpolicyid = enactedpolicyid;
    }

    public int getNominatedchancellorid() {
        return nominatedchancellorid;
    }

    public void setNominatedchancellorid(int nominatedchancellorid) {
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
