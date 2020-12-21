package eu.h2020.helios_social.modules.groupcommunications.api.messaging;

public enum MessageState {

	PENDING(1), DELIVERED(2), SEEN(3);

	private final int value;

	MessageState(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static MessageState fromValue(int value) {
		for (MessageState s : values()) if (s.value == value) return s;
		throw new IllegalArgumentException();
	}
}
