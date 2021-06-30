package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumManager;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.LocationForum;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.index.Indexer;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.LocationQuery;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.Queryable;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.SearchManager;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.TextQuery;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.utils.MathUtils;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;

public class SearchManagerImpl implements SearchManager<Transaction> {

    private static final double RELEVANCE_THRESHOLD = 0.0;

    private final Indexer forumIndexer;
    private final ForumManager forumManager;

    @Inject
    public SearchManagerImpl(Indexer forumIndexer, ForumManager forumManager) {
        this.forumIndexer = forumIndexer;
        this.forumManager = forumManager;
    }

    @Override
    public Map<Queryable, Double> execute(Transaction txn, TextQuery q) throws DbException, FormatException {
        switch (q.getEntityType()) {
            case FORUM:
                Map<String, Double> score = forumIndexer.search(txn, q.getQuery(), q.getContextId());
                return score.entrySet().stream().filter(e -> e.getValue() > RELEVANCE_THRESHOLD).collect(Collectors.toMap(
                        e -> {
                            try {
                                return (Queryable) forumManager.getForum(txn, e.getKey());
                            } catch (DbException dbException) {
                                dbException.printStackTrace();
                            } catch (FormatException formatException) {
                                formatException.printStackTrace();
                            }
                            return null;
                        }, e -> e.getValue(), (e1, e2) -> e1, LinkedHashMap::new));
            default:
                throw new UnsupportedOperationException(
                        "Queries of type " + q.getEntityType().toString() + " currently not supported");
        }
    }

    @Override
    public Map<Queryable, Double> execute(Transaction txn, LocationQuery q) throws DbException, FormatException {
        Map<Queryable, Double> score = new HashMap<>();
        switch (q.getEntityType()) {
            case FORUM:
                for (Object f : forumManager.getPublicLocationForums(txn, q.getContextId())) {
                    LocationForum locationForum = (LocationForum) f;
                    double distance = MathUtils.distanceHaversine(
                            locationForum.getLatitude(),
                            locationForum.getLongitude(),
                            q.getQueryLatitude(),
                            q.getQueryLongitude()
                    );
                    if (distance <= locationForum.getRadius()) {
                        score.put(locationForum, distance / q.getQueryRadius());
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException(
                        "Queries of type " + q.getEntityType().toString() + " currently not supported");
        }
        return score;
    }


}
