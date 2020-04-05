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
	private boolean isStarted;

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

	public void setInitialPlayerCount(Integer initialPlayerCount) {
		this.initialPlayerCount = initialPlayerCount;
	}

	public int getAvailableFascistPolicies() {
		return availableFascistPolicies;
	}

	public void setAvailableFascistPolicies(int availableFascistPolicies) {
		this.availableFascistPolicies = availableFascistPolicies;
	}

	public int getAvailableLiberalPolicies() {
		return availableLiberalPolicies;
	}

	public void setAvailableLiberalPolicies(int availableLiberalPolicies) {
		this.availableLiberalPolicies = availableLiberalPolicies;
	}

	public int getDiscardedFascistPolicies() {
		return discardedFascistPolicies;
	}

	public void setDiscardedFascistPolicies(int discardedFascistPolicies) {
		this.discardedFascistPolicies = discardedFascistPolicies;
	}

	public int getDiscardedLiberalPolicies() {
		return discardedLiberalPolicies;
	}

	public void setDiscardedLiberalPolicies(int discardedLiberalPolicies) {
		this.discardedLiberalPolicies = discardedLiberalPolicies;
	}

	public int getCardStackSeed() {
		return cardStackSeed;
	}

	public int getElectionTrackings() {
		return electionTrackings;
	}

	public boolean isStarted() {
		return isStarted;
	}

	public void setStarted(boolean started) {
		isStarted = started;
	}
}
