package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries;

import com.google.common.cache.Cache;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.Peer;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.Query;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryCache;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryForwarderManager;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryManager;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryResponse;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.Queryable;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.TextQuery;
import eu.h2020.helios_social.modules.groupcommunications_utils.lifecycle.IoExecutor;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.Event;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventListener;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.ForwardQueryEvent;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.QueryResponseReadyToSendEvent;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.LOCATION_QUERY_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.QUERY_RESPONSE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.TEXT_QUERY_PROTOCOL;
import static java.util.logging.Logger.getLogger;

public class QueryManagerImpl implements QueryManager, EventListener {
    private static final Logger LOG = getLogger(QueryManagerImpl.class.getName());

    private final CommunicationManager communicationManager;
    private final Executor ioExecutor;
    private final Cache<String, String> queryCache;
    private final QueryForwarderManager queryForwarderManager;

    @Inject
    public QueryManagerImpl(CommunicationManager communicationManager, @IoExecutor Executor ioExecutor,
                            @QueryCache Cache<String, String> queryCache, QueryForwarderManager queryForwarderManager) {
        this.communicationManager = communicationManager;
        this.ioExecutor = ioExecutor;
        this.queryCache = queryCache;
        this.queryForwarderManager = queryForwarderManager;
    }

    @Override
    public void sendQuery(Query query) throws DbException {
        LOG.info("Query info: " + query);
        queryCache.put(query.getQueryId(), "self");
        List<PeerId> nextHops = queryForwarderManager.getNextHops(new PeerId("self"), query);
        LOG.info("Next Hops: " + nextHops);
        for (PeerId peerId : nextHops) {
            forwardQuery(peerId, query);
        }
    }

    @Override
    public void forwardQuery(PeerId peerId, Query query) {
        ioExecutor.execute(() -> {
            try {
                if (query instanceof TextQuery) {
                    communicationManager.sendDirectMessage(TEXT_QUERY_PROTOCOL, peerId, query);
                } else {
                    communicationManager.sendDirectMessage(LOCATION_QUERY_PROTOCOL, peerId, query);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void sendQueryResponse(PeerId peerId, QueryResponse<Queryable> queryResponse) {
        LOG.info("QUERYRESPONSE: " + queryResponse.getEntities().entrySet().iterator().next().getValue().getQueryableType());
        ioExecutor.execute(() -> {
            try {
                communicationManager.sendDirectMessage(QUERY_RESPONSE_PROTOCOL, peerId, queryResponse);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void eventOccurred(Event e) {
        if (e instanceof ForwardQueryEvent) {
            ForwardQueryEvent event = (ForwardQueryEvent) e;
            LOG.info("forwarding query to neighbors..." + event.getForwardList());
            for (PeerId peerId : event.getForwardList()) {
                forwardQuery(peerId, event.getQuery());
            }
        } else if (e instanceof QueryResponseReadyToSendEvent) {
            QueryResponseReadyToSendEvent event = (QueryResponseReadyToSendEvent) e;
            LOG.info("sending query response...");
            sendQueryResponse(event.getPeerId(), event.getQueryResponse());
        }
    }
}
