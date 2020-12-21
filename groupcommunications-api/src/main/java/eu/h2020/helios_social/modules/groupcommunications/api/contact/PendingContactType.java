package eu.h2020.helios_social.modules.groupcommunications.api.contact;

public enum PendingContactType {

	INCOMING(0), OUTGOING(1);

	private final int value;

	PendingContactType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static PendingContactType fromValue(int value) {
		for (PendingContactType s : values()) if (s.value == value) return s;
		throw new IllegalArgumentException();
	}
}
