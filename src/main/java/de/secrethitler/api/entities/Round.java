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

	@ForeignKeyEntity("enactedPolicyId")
	private PolicyTypes policyType;

	public Round(int sequenceNumber, long gameId, long presidentId) {
		this.sequenceNumber = sequenceNumber;
		this.gameId = gameId;
		this.presidentId = presidentId;
	}

	public Round() {
	}

	public int getSequenceNumber() {
		return sequenceNumber;
    }

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
    }

	public long getGameId() {
		return gameId;
    }

	public void setGameId(long gameId) {
		this.gameId = gameId;
    }

	public long getPresidentId() {
		return presidentId;
    }

	public void setPresidentId(long presidentId) {
		this.presidentId = presidentId;
    }

	public Long getChancellorId() {
		return chancellorId;
    }

	public void setChancellorId(Long chancellorId) {
		this.chancellorId = chancellorId;
    }

	public Long getEnactedPolicyId() {
		return enactedPolicyId;
    }

	public void setEnactedPolicyId(Long enactedPolicyId) {
		this.enactedPolicyId = enactedPolicyId;
    }

	public long getNominatedChancellorId() {
		return nominatedChancellorId;
    }

	public void setNominatedChancellorId(long nominatedChancellorId) {
		this.nominatedChancellorId = nominatedChancellorId;
    }

	public PolicyTypes getPolicyType() {
		return policyType;
	}
}
