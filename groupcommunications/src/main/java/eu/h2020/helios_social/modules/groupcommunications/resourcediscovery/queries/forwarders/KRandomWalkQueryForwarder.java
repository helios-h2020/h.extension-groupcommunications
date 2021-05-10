package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.forwarders;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.Query;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryForwarder;

import static java.util.logging.Logger.getLogger;

public class KRandomWalkQueryForwarder extends QueryForwarder {
    private static Logger LOG = getLogger(KRandomWalkQueryForwarder.class.getName());

    private final SecureRandom secureRandom;
    private final static int k = 3;

    public KRandomWalkQueryForwarder(ContactManager contactManager) {
        super(contactManager);
        this.secureRandom = new SecureRandom();
    }

    @Override
    public List<PeerId> forward(PeerId peerId, Query query) throws DbException {
        List<PeerId> neighbors = contactManager.getContacts(query.getContextId())
                .stream()
                .map(contact -> new PeerId(contact.getId().getId()))
                .collect(Collectors.toList());

        LOG.info("Neighbors" + neighbors);

        neighbors.remove(peerId);

        List<PeerId> nextForwards = new ArrayList();
        int i = 0;
        while (i < k && !neighbors.isEmpty()) {
            int nextForward = secureRandom.nextInt(neighbors.size());
            nextForwards.add(neighbors.remove(nextForward));
        }

        LOG.info("next hops: " + nextForwards);
        return nextForwards;
    }
}
