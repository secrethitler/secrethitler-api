package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.ForeignKeyEntity;
import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;
import de.secrethitler.api.enums.PolicyTypes;

/**
 * @author Vladislav Denzel
 */
@TableName("round")
public class Round extends BaseEntity {

	private int sequenceNumber;
	private long gameId;
	private long presidentId;
	private Long chancellorId;
	private Long enactedPolicyId;
	private Long nominatedChancellorId;
	private boolean isSpecialElectionRound;

	@ForeignKeyEntity("gameId")
	private Game game;

	@ForeignKeyEntity("enactedPolicyId")
	private PolicyTypes enactedPolicy;

	public Round(int sequenceNumber, long gameId, long presidentId, boolean isSpecialElectionRound) {
		this.sequenceNumber = sequenceNumber;
		this.gameId = gameId;
		this.presidentId = presidentId;
		this.isSpecialElectionRound = isSpecialElectionRound;
	}

	public Round(int sequenceNumber, long gameId, long presidentId) {
		this(sequenceNumber, gameId, presidentId, false);
	}

	public Round() {
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public long getGameId() {
		return gameId;
	}

	public Game getGame() {
		return game;
	}

	public long getPresidentId() {
		return presidentId;
	}

	public Long getChancellorId() {
		return chancellorId;
	}

	public Long getEnactedPolicyId() {
		return enactedPolicyId;
	}

	public Long getNominatedChancellorId() {
		return nominatedChancellorId;
	}

	public PolicyTypes getEnactedPolicy() {
		return enactedPolicy;
	}

	public boolean isSpecialElectionRound() {
		return isSpecialElectionRound;
	}
}
