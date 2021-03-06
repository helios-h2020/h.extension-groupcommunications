package eu.h2020.helios_social.modules.groupcommunications.contact.connection;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;
import eu.h2020.helios_social.modules.groupcommunications_utils.identity.IdentityManager;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.Contact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContactType;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.conversation.ConversationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.ProfileManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.lifecycle.IoExecutor;

import static eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionConstants.CONNECTIONS_RECEIVER_ID;

public class ConnectionManagerImpl implements ConnectionManager {
    private final Logger LOG = Logger.getLogger(ConnectionManagerImpl.class.getName());

    private final ContactManager contactManager;
    private final Executor ioExecutor;
    private final ProfileManager profileManager;
    private final CommunicationManager communicationManager;
    private final IdentityManager identityManager;
    private final ConversationManager conversationManager;

    @Inject
    public ConnectionManagerImpl(ContactManager contactManager,
                                 @IoExecutor Executor ioExecutor,
                                 ProfileManager profileManager,
                                 IdentityManager identityManager,
                                 CommunicationManager communicationManager,
                                 ConversationManager conversationManager) {
        this.contactManager = contactManager;
        this.ioExecutor = ioExecutor;
        this.profileManager = profileManager;
        this.identityManager = identityManager;
        this.communicationManager = communicationManager;
        this.conversationManager = conversationManager;
    }

    @Override
    public void sendConnectionRequest(PendingContact pendingContact) throws DbException {
        LOG.info("trying to add pending contact");
        contactManager.addPendingContact(pendingContact);
        LOG.info("pending contact added");
        ConnectionInfo connectionInfo =
                new ConnectionInfo(identityManager.getIdentity().getAlias(),
                                   profileManager.getProfile("All").getProfilePic(),
                                   pendingContact.getTimestamp(), identityManager.getPublicKey().getEncoded())
                        .setMessage(pendingContact.getMessage());
        sendMessage(CONNECTIONS_RECEIVER_ID, pendingContact.getId(), connectionInfo);
    }

    @Override
    public void acceptConnectionRequest(PendingContact pendingContact) {
        try {
            if (pendingContact.getPendingContactType()
                    .equals(PendingContactType.INCOMING)) {
                ConnectionInfo connectionInfo =
                        new ConnectionInfo(
                                identityManager.getIdentity().getAlias(),
                                profileManager.getProfile("All").getProfilePic(),
                                pendingContact.getTimestamp(),
                                identityManager.getPublicKey().getEncoded());
                Group group = new Group(UUID.randomUUID().toString(), "All",
                                        GroupType.PrivateConversation);
                contactManager.addContact(new Contact(pendingContact.getId(),
                        pendingContact.getAlias(), pendingContact.getProfilePicture(),
                        pendingContact.getPublicKey()));
                conversationManager
                        .addContactGroup(pendingContact.getId(), group);
                contactManager.deletePendingContact(pendingContact.getId());
                sendMessage(CONNECTIONS_RECEIVER_ID,
                            pendingContact.getId(), connectionInfo
                                    .setConversationInfo(group.getId(),
                                                         group.getContextId()));
            }
        } catch (DbException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }

    }

    @Override
    public void rejectConnectionRequest(PendingContact pendingContact) {
        try {
            contactManager.deletePendingContact(pendingContact.getId());
            sendMessage(CONNECTIONS_RECEIVER_ID, pendingContact.getId(), new ConnectionInfo());
        } catch (DbException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void sendMessage(String protocol, ContactId contactId, AbstractMessage message) {
        ioExecutor.execute(() -> {
            communicationManager.sendDirectMessage(
                    protocol,
                    contactId,
                    message
            );
        });
    }
}
