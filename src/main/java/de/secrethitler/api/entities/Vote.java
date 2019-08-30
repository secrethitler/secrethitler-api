package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;

/**
 * @author Vladislav Denzel
 */
@TableName("linkedusergamerole")
public class Vote extends BaseEntity {

	private long userId;
	private long roundId;
	private boolean votedChancellor;

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
