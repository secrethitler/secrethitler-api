package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.ForeignKeyEntity;
import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Collin Alpert
 */
@TableName("game")
public class Game extends BaseEntity {

	private long creatorid;
	private String channelname;

	public Game(long creatorid, String channelname) {
		this.creatorid = creatorid;
		this.channelname = channelname;
	}

	public Game() {
	}

	@ForeignKeyEntity("creatorid")
	private User creator;

	public long getCreatorid() {
		return creatorid;
	}

	public void setCreatorid(long creatorid) {
		this.creatorid = creatorid;
	}

	public User getCreator() {
		return creator;
	}

	public String getChannelname() {
		return channelname;
	}

	public void setChannelname(String channelname) {
		this.channelname = channelname;
	}
}
