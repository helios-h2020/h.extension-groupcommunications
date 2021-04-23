package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.forwarders;

import java.lang.reflect.Array;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.Query;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryForwarder;

public class RandomWalkQueryForwarder extends QueryForwarder {

    private final SecureRandom secureRandom;

    public RandomWalkQueryForwarder(ContactManager contactManager) {
        super(contactManager);
        this.secureRandom = new SecureRandom();
    }

    @Override
    public List<PeerId> forward(PeerId peerId, Query query) throws DbException {
        List<PeerId> neighbors = contactManager.getContacts(query.getContextId())
                .stream()
                .map(contact -> new PeerId(contact.getId().getId()))
                .collect(Collectors.toList());

        neighbors.remove(peerId);

        List<PeerId> nextForwards = new ArrayList();
        if (neighbors.size() > 0) {
            int nextForward = secureRandom.nextInt(neighbors.size());
            nextForwards.add(neighbors.get(nextForward));
        }

        return nextForwards;
    }
}
