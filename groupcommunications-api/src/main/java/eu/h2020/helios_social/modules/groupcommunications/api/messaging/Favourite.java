package eu.h2020.helios_social.modules.groupcommunications.api.messaging;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;

public class Favourite {

	private String messageId;
	private String author;
	private String body;
	private long timestamp;

	public Favourite(String messageId, String author, String body,
			long timestamp) {
		this.messageId = messageId;
		this.author = author;
		this.body = body;
		this.timestamp = timestamp;
	}

	public String getMessageId() {
		return messageId;
	}

	public String getAuthor() {
		return author;
	}

	public String getTextMessage() {
		return body;
	}

	public long getTimestamp() {
		return timestamp;
	}
}

