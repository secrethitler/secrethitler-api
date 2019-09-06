package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Collin Alpert
 */
@TableName("game")
public class Game extends BaseEntity {

	private long creatorId;
	private String channelName;
	private Integer initialPlayerCount;
	private int availableFascistPolicies;
	private int availableLiberalPolicies;
	private int discardedFascistPolicies;
	private int discardedLiberalPolicies;

	public Game(long creatorId, String channelName, int availableFascistPolicies, int availableLiberalPolicies) {
		this.creatorId = creatorId;
		this.channelName = channelName;
		this.availableFascistPolicies = availableFascistPolicies;
		this.availableLiberalPolicies = availableLiberalPolicies;
	}

	public Game() {
	}

	public long getCreatorId() {
		return creatorId;
	}

	public String getChannelName() {
		return channelName;
	}

	public Integer getInitialPlayerCount() {
		return initialPlayerCount;
	}

	public int getAvailableFascistPolicies() {
		return availableFascistPolicies;
	}

	public int getAvailableLiberalPolicies() {
		return availableLiberalPolicies;
	}

	public int getDiscardedFascistPolicies() {
		return discardedFascistPolicies;
	}

	public int getDiscardedLiberalPolicies() {
		return discardedLiberalPolicies;
	}
}
