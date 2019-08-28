package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.ForeignKeyEntity;
import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Collin Alpert
 */
@TableName("game")
public class Game extends BaseEntity {

	private long creatorId;

	@ForeignKeyEntity("creatorId")
	private User creator;

	private String channelName;
	private Integer initialPlayerCount;

	public Game() {
	}

	public Game(long creatorId, String channelName) {
		this.creatorId = creatorId;
		this.channelName = channelName;
	}

	public long getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(long creatorId) {
		this.creatorId = creatorId;
	}

	public User getCreator() {
		return creator;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public Integer getInitialPlayerCount() {
		return initialPlayerCount;
	}

	public void setInitialPlayerCount(Integer initialPlayerCount) {
		this.initialPlayerCount = initialPlayerCount;
	}
}
