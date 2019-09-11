package de.secrethitler.api.entities;

import com.github.collinalpert.java2db.annotations.ForeignKeyEntity;
import com.github.collinalpert.java2db.annotations.TableName;
import com.github.collinalpert.java2db.entities.BaseEntity;
import de.secrethitler.api.enums.PolicyTypes;

/**
 * @author Collin Alpert
 */
@TableName("linkedRoundPolicySuggestion")
public class LinkedRoundPolicySuggestion extends BaseEntity {

	private long roundId;
	private long policyId;
	private boolean isDiscarded;

	@ForeignKeyEntity("policyId")
	private PolicyTypes policyType;

	public LinkedRoundPolicySuggestion(long roundId, long policyId) {
		this.roundId = roundId;
		this.policyId = policyId;
		this.isDiscarded = false;
	}

	public LinkedRoundPolicySuggestion() {
	}

	public long getRoundId() {
		return roundId;
	}

	public long getPolicyId() {
		return policyId;
	}

	public boolean isDiscarded() {
		return isDiscarded;
	}

	public PolicyTypes getPolicyType() {
		return policyType;
	}
}
