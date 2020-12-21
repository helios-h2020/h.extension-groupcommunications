package eu.h2020.helios_social.modules.groupcommunications.api.context.sharing;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.context.ContextType;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.sharing.Invitation;

public class ContextInvitation extends Invitation implements AbstractMessage {

	private ContextType contextType;

	public ContextInvitation(
			ContactId contactId, String contextId, String name,
			ContextType contextType, String json, long timestamp,
			boolean incoming) {
		super(contactId, contextId, name, json, timestamp, incoming);
		this.contextType = contextType;
	}

	public ContextType getContextType() {
		return contextType;
	}

}
