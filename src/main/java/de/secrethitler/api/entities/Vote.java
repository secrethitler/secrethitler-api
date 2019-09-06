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
	private boolean votedForChancellor;

	public Vote(long userId, long roundId, boolean votedForChancellor) {
		this.userId = userId;
		this.roundId = roundId;
		this.votedForChancellor = votedForChancellor;
	}

	public Vote() {
	}

	public long getUserId() {
		return userId;
	}

	public long getRoundId() {
		return roundId;
	}

	public boolean getVotedForChancellor() {
		return votedForChancellor;
	}
}
