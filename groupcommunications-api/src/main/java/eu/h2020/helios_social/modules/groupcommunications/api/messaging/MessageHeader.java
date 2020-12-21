package eu.h2020.helios_social.modules.groupcommunications.api.messaging;

import eu.h2020.helios_social.modules.groupcommunications.api.conversation.ConversationMessageVisitor;

public class MessageHeader {

	private final String messageId;
	private final String groupId;
	private final long timestamp;
	private boolean incoming;
	private boolean favourite;
	private MessageState state;
	private final Message.Type messageType;

	public MessageHeader(String messageId, String groupId, long timestamp,
			MessageState state, boolean incoming, boolean favourite,
			Message.Type messageType) {
		this.messageId = messageId;
		this.groupId = groupId;
		this.timestamp = timestamp;
		this.incoming = incoming;
		this.state = state;
		this.favourite = favourite;
		this.messageType = messageType;
	}

	/**
	 * Returns the ID of the message.
	 */
	public String getMessageId() {
		return messageId;
	}

	public String getGroupId() {
		return groupId;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public boolean isIncoming() {
		return incoming;
	}

	/**
	 * Returns message state.
	 */
	public MessageState getMessageState() {
		return state;
	}

	/**
	 * Returns true if the message has been liked by the contact.
	 */
	public boolean isFavourite() {
		return favourite;
	}

	public Message.Type getMessageType() {
		return messageType;
	}

	public void setMessageState(MessageState state) {
		this.state = state;
	}

	public <T> T accept(ConversationMessageVisitor<T> v) {
		return v.visitMessageHeader(this);
	}

}

