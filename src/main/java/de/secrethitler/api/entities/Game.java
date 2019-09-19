package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Collin Alpert
 */
@TableName("game")
public class Game extends BaseEntity {

	private Long creatorId;
	private String channelName;
	private Integer initialPlayerCount;
	private int availableFascistPolicies;
	private int availableLiberalPolicies;
	private int discardedFascistPolicies;
	private int discardedLiberalPolicies;
	private int cardStackSeed;
	private int electionTrackings;

	public Game(String channelName, int availableFascistPolicies, int availableLiberalPolicies, int cardStackSeed) {
		this.channelName = channelName;
		this.availableFascistPolicies = availableFascistPolicies;
		this.availableLiberalPolicies = availableLiberalPolicies;
		this.cardStackSeed = cardStackSeed;
	}

	public Game() {
	}

	public Long getCreatorId() {
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

	public int getCardStackSeed() {
		return cardStackSeed;
	}

	public int getElectionTrackings() {
		return electionTrackings;
	}
}
