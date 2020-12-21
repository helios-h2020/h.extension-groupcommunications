package eu.h2020.helios_social.modules.groupcommunications.contact.connection;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosMessagingReceiver;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosNetworkAddress;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.Contact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContactFactory;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContactType;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.conversation.ConversationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;

import static eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionConstants.CONNECTIONS_RECEIVER_ID;
import static java.util.logging.Logger.getLogger;

public class ConnectionRequestReceiver
		implements HeliosMessagingReceiver {
	private static final Logger LOG =
			getLogger(ConnectionRequestReceiver.class.getName());

	private final ContactManager contactManager;
	private final PendingContactFactory pendingContactFactory;
	private final ConversationManager conversationManager;

	@Inject
	public ConnectionRequestReceiver(ContactManager contactManager,
			ConversationManager conversationManager,
			PendingContactFactory pendingContactFactory) {
		this.contactManager = contactManager;
		this.pendingContactFactory = pendingContactFactory;
		this.conversationManager = conversationManager;
	}

	@Override
	public void receiveMessage(
			@NotNull HeliosNetworkAddress heliosNetworkAddress,
			@NotNull String protocolId,
			@NotNull FileDescriptor fileDescriptor) {
		if (!protocolId.equals(CONNECTIONS_RECEIVER_ID)) return;
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		try (FileInputStream fileInputStream = new FileInputStream(
				fileDescriptor)) {
			int byteRead;
			while ((byteRead = fileInputStream.read()) != -1) {
				ba.write(byteRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		receiveMessage(heliosNetworkAddress, protocolId, ba.toByteArray());
	}

	@Override
	public void receiveMessage(
			@NotNull HeliosNetworkAddress heliosNetworkAddress,
			@NotNull String protocolId, @NotNull byte[] data) {
		if (!protocolId.equals(CONNECTIONS_RECEIVER_ID)) return;
		String stringMessage = new String(data, StandardCharsets.UTF_8);
		LOG.info(CONNECTIONS_RECEIVER_ID + ": " + stringMessage);
		ConnectionInfo connectionInfo =
				new Gson().fromJson(stringMessage, ConnectionInfo.class);
		try {
			PendingContact pendingContact = contactManager.getPendingContact(
					new ContactId(heliosNetworkAddress.getNetworkId()));
			if (pendingContact == null && connectionInfo.getAlias() != null) {
				pendingContact = pendingContactFactory
						.createIncomingPendingContact(
								heliosNetworkAddress.getNetworkId(),
								connectionInfo);
				contactManager.addPendingContact(pendingContact);
			} else if (pendingContact.getPendingContactType().equals(
					PendingContactType.OUTGOING) &&
					connectionInfo.getAlias() != null) {
				ContactId contactId =
						new ContactId(heliosNetworkAddress.getNetworkId());

				contactManager.addContact(new Contact(contactId,
						connectionInfo.getAlias()));
				Group group = new Group(connectionInfo.getGroupId(),
						connectionInfo.getContextId(),
						GroupType.PrivateConversation);
				conversationManager.addContactGroup(contactId, group);
				contactManager.deletePendingContact(pendingContact.getId());
			} else if (pendingContact.getPendingContactType().equals(
					PendingContactType.OUTGOING) &&
					connectionInfo.getAlias() == null) {
				contactManager.deletePendingContact(pendingContact.getId());
			}
		} catch (DbException e) {
			e.printStackTrace();
		}

	}
}
