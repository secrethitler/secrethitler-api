package de.secrethitler.api.enums;

/**
 * @author Collin Alpert
 */
public enum ExecutiveActionTypes {

	POLICY_PEEK("policyPeek"),
	EXECUTE_PLAYER("executePlayer"),
	LOYALTY_INVESTIGATION("loyaltyInvestigation"),
	SPECIAL_ELECTION("specialElection");

	private final String pusherEventName;

	ExecutiveActionTypes(String pusherEventName) {
		this.pusherEventName = pusherEventName;
	}

	public String getPusherEventName() {
		return pusherEventName;
	}
}
