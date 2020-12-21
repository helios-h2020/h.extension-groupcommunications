package eu.h2020.helios_social.modules.groupcommunications.contact.connection;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.happ.helios.talk.api.identity.IdentityManager;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.Contact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContactType;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.conversation.ConversationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;

import static eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionConstants.CONNECTIONS_RECEIVER_ID;

public class ConnectionManagerImpl implements ConnectionManager {

	private final ContactManager contactManager;
	private final CommunicationManager communicationManager;
	private final IdentityManager identityManager;
	private final ConversationManager conversationManager;

	@Inject
	public ConnectionManagerImpl(ContactManager contactManager,
			IdentityManager identityManager,
			CommunicationManager communicationManager,
			ConversationManager conversationManager) {
		this.contactManager = contactManager;
		this.identityManager = identityManager;
		this.communicationManager = communicationManager;
		this.conversationManager = conversationManager;
	}

	@Override
	public void sendConnectionRequest(PendingContact pendingContact) {
		try {
			contactManager.addPendingContact(pendingContact);
			ConnectionInfo connectionInfo =
					new ConnectionInfo(identityManager.getIdentity().getAlias(),
							pendingContact.getTimestamp())
							.setMessage(pendingContact.getMessage());
			communicationManager.sendDirectMessage(CONNECTIONS_RECEIVER_ID,
					pendingContact.getId(), connectionInfo);

		} catch (DbException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void acceptConnectionRequest(PendingContact pendingContact) {
		try {
			if (pendingContact.getPendingContactType()
					.equals(PendingContactType.INCOMING)) {
				ConnectionInfo connectionInfo =
						new ConnectionInfo(
								identityManager.getIdentity().getAlias(),
								pendingContact.getTimestamp());
				Group group = new Group(UUID.randomUUID().toString(), "All",
						GroupType.PrivateConversation);
				communicationManager.sendDirectMessage(CONNECTIONS_RECEIVER_ID,
						pendingContact.getId(), connectionInfo
								.setConversationInfo(group.getId(),
										group.getContextId()));
				contactManager.addContact(new Contact(pendingContact.getId(),
						pendingContact.getAlias()));
				conversationManager
						.addContactGroup(pendingContact.getId(), group);
				contactManager.deletePendingContact(pendingContact.getId());
			}
		} catch (DbException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void rejectConnectionRequest(PendingContact pendingContact) {
		try {
			communicationManager.sendDirectMessage(CONNECTIONS_RECEIVER_ID,
					pendingContact.getId(), new ConnectionInfo());
			contactManager.deletePendingContact(pendingContact.getId());
		} catch (DbException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}
}
