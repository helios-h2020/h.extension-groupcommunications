package eu.h2020.helios_social.modules.groupcommunications.api.group;

public enum GroupType {

	PrivateConversation(0), PrivateGroup(1), PublicForum(2), ProtectedForum(3),
	SecretForum(4);

	private int value;

	GroupType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static GroupType fromValue(int value) {
		for (GroupType g : values()) if (g.value == value) return g;
		throw new IllegalArgumentException();
	}
}
