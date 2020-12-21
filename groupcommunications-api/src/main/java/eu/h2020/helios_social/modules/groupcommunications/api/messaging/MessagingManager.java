package eu.h2020.helios_social.modules.groupcommunications.api.messaging;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;

public interface MessagingManager {

	MessageHeader sendPrivateMessage(ContactId contactId, String contextId,
			Message privateMessage)
			throws DbException;

	MessageHeader sendPrivateGroupMessage(PrivateGroup privateGroup,
			GroupMessage groupMessage)
			throws DbException, FormatException;
}
