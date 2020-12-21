package eu.h2020.helios_social.modules.groupcommunications.messaging.event;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;


/**
 * An event that is broadcast when a new private message is received.
 */
@NotNullByDefault
public class PrivateMessageReceivedEvent
		extends Event {

	private Message.Type msgType;
	private MessageHeader messageHeader;
	private ContactId contactId;

	public PrivateMessageReceivedEvent(MessageHeader messageHeader,
			ContactId contactId) {
		this.messageHeader = messageHeader;
		this.contactId = contactId;
	}

	public Message.Type getMessageType() {
		return msgType;
	}

	public ContactId getContactId() {
		return contactId;
	}

	public String getGroupId() {
		return messageHeader.getGroupId();
	}

	public MessageHeader getMessageHeader() {
		return messageHeader;
	}

	public void setMessageType(Message.Type messageType) {
		this.msgType = messageType;
	}

}
