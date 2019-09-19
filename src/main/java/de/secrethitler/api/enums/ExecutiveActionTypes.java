package de.secrethitler.api.enums;

/**
 * @author Collin Alpert
 */
public enum ExecutiveActionTypes {

	POLICY_PEEK("policy_peek"),
	EXECUTE_PLAYER("execute_player"),
	LOYALTY_INVESTIGATION("loyalty_investigation"),
	SPECIAL_ELECTION("special_election");

	private final String pusherEventName;

	ExecutiveActionTypes(String pusherEventName) {
		this.pusherEventName = pusherEventName;
	}

	public String getPusherEventName() {
		return pusherEventName;
	}
}
