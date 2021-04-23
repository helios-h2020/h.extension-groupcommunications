package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.forwarders;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.Query;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryForwarder;

public class FloodingQueryForwarder extends QueryForwarder {

    public FloodingQueryForwarder(ContactManager contactManager) {
        super(contactManager);
    }

    @Override
    public List<PeerId> forward(PeerId peerId, Query query) throws DbException {

        List<PeerId> neighbors = contactManager.getContacts(query.getContextId())
                .stream()
                .map(contact -> new PeerId(contact.getId().getId()))
                .collect(Collectors.toList());

        neighbors.remove(peerId);
        return neighbors;
    }
}
