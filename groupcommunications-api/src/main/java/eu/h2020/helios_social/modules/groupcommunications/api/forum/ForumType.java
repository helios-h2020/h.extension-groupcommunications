package eu.h2020.helios_social.modules.groupcommunications.api.forum;

public enum ForumType {

	GENERAL(0), LOCATION(1), SEASONAL(3), SPATIOTEMPORAL(4);

	private final int value;

	ForumType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static ForumType fromValue(int value) {
		for (ForumType s : values()) if (s.value == value) return s;
		throw new IllegalArgumentException();
	}
}
