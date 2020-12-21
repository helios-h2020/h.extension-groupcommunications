package eu.h2020.helios_social.happ.helios.talk.api.sync.event;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageState;

/**
 * An event that is broadcast when a message is added to the database.
 */
@Immutable
@NotNullByDefault
public class MessageAddedEvent extends Event {

	private final Message message;
	@Nullable
	private final boolean incoming;
	private final MessageState state;

	public MessageAddedEvent(Message message, boolean incoming,
			MessageState state) {
		this.message = message;
		this.incoming = incoming;
		this.state = state;
	}

	/**
	 * Returns the message that was added.
	 */
	public Message getMessage() {
		return message;
	}


	public boolean isIncoming() {
		return incoming;
	}

	@Nullable
	public MessageState getState() {
		return state;
	}
}
