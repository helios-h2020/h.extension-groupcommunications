package eu.h2020.helios_social.modules.groupcommunications.api.contact.connection;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContact;

public interface ConnectionManager {

	void sendConnectionRequest(PendingContact pendingContact);

	void acceptConnectionRequest(PendingContact pendingContact);

	void rejectConnectionRequest(PendingContact pendingContact);
}
