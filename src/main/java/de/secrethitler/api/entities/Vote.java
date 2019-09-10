package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Vladislav Denzel
 */
@TableName("vote")
public class Vote extends BaseEntity {

	private long userId;
	private long roundId;
	private boolean votedChancellor;

	public Vote(long userId, long roundId, boolean votedChancellor) {
		this.userId = userId;
		this.roundId = roundId;
		this.votedChancellor = votedChancellor;
	}

	public Vote() {
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getRoundId() {
		return roundId;
	}

	public void setRoundId(long roundId) {
		this.roundId = roundId;
	}

	public boolean getVotedChancellor() {
		return votedChancellor;
	}

	public void setVotedChancellor(boolean votedChancellor) {
		this.votedChancellor = votedChancellor;
	}

}
