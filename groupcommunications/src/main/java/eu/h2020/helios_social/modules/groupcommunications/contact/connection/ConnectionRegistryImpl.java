package eu.h2020.helios_social.modules.groupcommunications.contact.connection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionRegistry;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;

public class ConnectionRegistryImpl implements ConnectionRegistry {

    private final ContactManager contactManager;
    private final CommunicationManager communicationManager;

    @Inject
    public ConnectionRegistryImpl(ContactManager contactManager, CommunicationManager communicationManager) {
        this.contactManager = contactManager;
        this.communicationManager = communicationManager;
    }

    @Override
    public boolean isConnected(ContactId contactId) {
        List<ContactId> cidAsList = new ArrayList();
        cidAsList.add(contactId);
        return !communicationManager.getOnlineContacts(cidAsList).isEmpty();
    }

    @Override
    public boolean isConnected(PeerId peerId) {
        List<PeerId> pidAsList = new ArrayList();
        pidAsList.add(peerId);
        return !communicationManager.getOnlineContacts(pidAsList).isEmpty();
    }

    @Override
    public List<ContactId> getConnectedContacts() throws DbException {
        return communicationManager.getOnlineContacts(
                contactManager.getContacts().stream().map(contact -> {
                    return contact.getId();
                }).collect(Collectors.toList())
        );
    }
}
