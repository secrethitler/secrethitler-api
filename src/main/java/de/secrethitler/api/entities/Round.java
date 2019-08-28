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
	private Integer enactedPolicyId;
	private long nominatedChancellorId;

	@ForeignKeyEntity("gameId")
    private Game game;

	@ForeignKeyEntity("enactedPolicyId")
	private PolicyTypes policyType;

	@ForeignKeyEntity("presidentId")
    private User president;

	@ForeignKeyEntity("chancellorId")
    private User chancellor;

	@ForeignKeyEntity("nominatedChancellorId")
	private User nominatedChancellor;


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

	public Integer getEnactedPolicyId() {
		return enactedPolicyId;
    }

	public void setEnactedPolicyId(Integer enactedPolicyId) {
		this.enactedPolicyId = enactedPolicyId;
    }

	public long getNominatedChancellorId() {
		return nominatedChancellorId;
    }

	public void setNominatedChancellorId(long nominatedChancellorId) {
		this.nominatedChancellorId = nominatedChancellorId;
    }

    public Game getGame() {
        return game;
    }

	public PolicyTypes getPolicyType() {
		return policyType;
	}

	public User getPresident() {
        return president;
    }

    public User getChancellor() {
        return chancellor;
    }

	public User getNominatedChancellor() {
		return nominatedChancellor;
    }

}
