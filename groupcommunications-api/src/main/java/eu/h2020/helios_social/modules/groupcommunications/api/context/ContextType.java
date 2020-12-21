package eu.h2020.helios_social.modules.groupcommunications.api.context;

public enum ContextType {

	GENERAL(0), LOCATION(1), TIME(3), SPATIOTEMPORAL(4);

	private final int value;

	ContextType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static ContextType fromValue(int value) {
		for (ContextType s : values()) if (s.value == value) return s;
		throw new IllegalArgumentException();
	}
}
