package eu.h2020.helios_social.modules.groupcommunications.api.contact;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionInfo;

public interface PendingContactFactory {

	/**
	 * Creates an outgoing {@link PendingContact} from the given the helios-peer-id and alias.
	 */
	PendingContact createOutgoingPendingContact(String peerId, String alias,
			String message);

	/**
	 * Creates an outgoing {@link PendingContact} from the given the helios-peer-id and alias.
	 */
	PendingContact createIncomingPendingContact(String peerId,
			ConnectionInfo connectionInfo);


}
