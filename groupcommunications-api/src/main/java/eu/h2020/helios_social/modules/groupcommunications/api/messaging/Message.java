package eu.h2020.helios_social.modules.groupcommunications.api.messaging;

import org.jetbrains.annotations.NotNull;


/**
 * Represents a Message
 */
public class Message implements AbstractMessage {

	public enum Type {
		TEXT(0), ATTACHMENT(1), VIDEOCALL(2), ACK(3);

		private final int value;

		Type(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public static Type fromValue(int value) {
			for (Type s : values()) if (s.value == value) return s;
			throw new IllegalArgumentException();
		}
	}

	private final String id;
	private final String groupId;
	private final long timestamp;
	private String preferences;
	private String body;
	private String mediaFileName;
	private final Message.Type message_type;

	/**
	 * Contracts a new instance of Message
	 *
	 * @param id            message identifier
	 * @param groupId       group Identifier
	 * @param timestamp
	 * @param body
	 * @param mediaFileName
	 */
	public Message(@NotNull String id, @NotNull String groupId, long timestamp,
			String body, String mediaFileName, Message.Type type) {
		this.id = id;
		this.groupId = groupId;
		this.timestamp = timestamp;
		this.body = body;
		this.mediaFileName = mediaFileName;
		this.message_type = type;
	}

	/**
	 * Contracts a new instance of Message
	 *
	 * @param id        message identifier
	 * @param groupId   group Identifier
	 * @param timestamp
	 * @param body
	 */
	public Message(@NotNull String id, @NotNull String groupId, long timestamp,
			String body, Message.Type type) {
		this.id = id;
		this.groupId = groupId;
		this.timestamp = timestamp;
		this.body = body;
		this.message_type = type;
	}

	public String getMessageBody() {
		return body;
	}

	public Message.Type getMessageType() {
		return message_type;
	}

	public String getMediaFileName() {
		return mediaFileName;
	}

	/**
	 * @return the Message Identifier
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return Conversation Identifier of the group the message shared to
	 */
	public String getGroupId() {
		return groupId;
	}

	public void setPreferences(String preferences) {
		this.preferences = preferences;
	}

	public String getPreferences() {
		return preferences;
	}

	/**
	 * @return epoch timestamp of the message
	 */
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Message && id.equals(((Message) o).getId());
	}
}
