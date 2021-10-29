package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.forwarders;

import android.util.Log;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.Query;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryForwarder;

import static java.util.logging.Logger.getLogger;

public class BiasedWalkQueryForwarder extends QueryForwarder {
    private static Logger LOG = getLogger(BiasedWalkQueryForwarder.class.getName());

    private final SecureRandom secureRandom;

    // forward probabilities per user in the forwarder's context
    // provided by the ResourceDiscoveryManager
    private final HashMap<PeerId,Double> distribution;

    public BiasedWalkQueryForwarder(ContactManager contactManager, HashMap<PeerId,Double> distribution) {
        super(contactManager);

        this.distribution = distribution;

        this.secureRandom = new SecureRandom();

    }

    // select a random index according to the forward probabilities
    private int randomSelect(List<Double> probabilities) {

        float randomFloat = secureRandom.nextFloat();
        float sum = 0f;
        int pos = -1;
        for (int i=0; i<probabilities.size(); i++) {
            sum += probabilities.get(i);
            if (randomFloat < sum) {
                pos = i;
                break;
            }
        }
        return pos;
    }

	// normalize a sequence so that is sums to 1
    private static List<Double> normalize(List<Double> sequence) {
        double sum = 0.0;
        for (Double num : sequence) {
            sum += num;
        }
        List<Double> normedSequence = new ArrayList<>(sequence.size());
        for (Double num : sequence) {
            normedSequence.add(num / sum);
        }
        return normedSequence;
    }

	private HashMap<PeerId,Double> syncWithContactManager(HashMap<PeerId, Double> distribution, Query query) throws DbException {

		Set<PeerId> neighbors = contactManager.getContacts(query.getContextId())
                .stream()
                .map(contact -> new PeerId(contact.getId().getId()))
                .collect(Collectors.toSet());
        LOG.info("Neighbors" + neighbors);
		Set<PeerId> nonNeighbors = new HashSet<>(distribution.keySet());
        LOG.info("nonNeighbors" + nonNeighbors);
		nonNeighbors.removeAll(neighbors);
        LOG.info("nonNeighbors updated" + nonNeighbors);
        HashMap<PeerId,Double> syncedNeighbors = new HashMap<>();
        if (nonNeighbors.size() > 0) {
            // shouldnt happen
            LOG.info("Try to forward query to non-contacts");
            return syncedNeighbors;
        }
		double syncedProbSum = 0.0;
		HashMap<PeerId,Double> unsyncedNeighbors = new HashMap<>();
		for (PeerId peerId : neighbors) {
			if (distribution.containsKey(peerId)) {
				syncedNeighbors.put(peerId, distribution.get(peerId));
				syncedProbSum += distribution.get(peerId);
			} else {
				unsyncedNeighbors.put(peerId, 1.0/neighbors.size());
			}
		}
		double targetSyncedProbSum = syncedNeighbors.size() / (double) neighbors.size();
		for (PeerId peerId : syncedNeighbors.keySet()) {
			syncedNeighbors.replace(peerId, syncedNeighbors.get(peerId) / syncedProbSum * targetSyncedProbSum);
		}
        syncedNeighbors.putAll(unsyncedNeighbors);
		return syncedNeighbors;
	}

    @Override
    public List<PeerId> forward(PeerId peerId, Query query) throws DbException {

		HashMap<PeerId,Double> syncedDistribution = syncWithContactManager(distribution,query);
        LOG.info("syncedDistribution" + String.valueOf(syncedDistribution.entrySet()));
        List<PeerId> neighbors = new ArrayList<>();
        List<Double> probabilities = new ArrayList<>();
        for (Entry e: syncedDistribution.entrySet()) {
            if (e.getKey().equals(peerId)) {
                continue;
            }
            neighbors.add((PeerId) e.getKey());
            probabilities.add((Double) e.getValue());
        }
        // adjust probabilities
        if (distribution.containsKey(peerId)) {
            probabilities = normalize(probabilities);
        }

        // choose 1 neighbor randomly
        List<PeerId> nextForwards = new ArrayList<>();
        if (neighbors.size() > 0) {
            int nextForward = randomSelect(probabilities);
            nextForwards.add(neighbors.get(nextForward));
        }

        return nextForwards;
    }
}
