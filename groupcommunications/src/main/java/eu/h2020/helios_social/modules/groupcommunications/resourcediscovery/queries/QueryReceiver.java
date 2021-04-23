package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries;

import com.google.common.cache.Cache;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosMessagingReceiver;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosNetworkAddress;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.LocationQuery;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryCache;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryForwarderManager;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryResponse;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryResultsCache;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.Queryable;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.SearchManager;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.TextQuery;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.ForwardQueryEvent;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.QueryResponseReadyToSendEvent;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.TEXT_QUERY_PROTOCOL;
import static java.util.logging.Logger.getLogger;

public class QueryReceiver implements HeliosMessagingReceiver {
    private static final Logger LOG =
            getLogger(QueryReceiver.class.getName());

    private final Cache<String, String> queryCache;
    private final SearchManager searchManager;
    private final Cache<String, QueryResponse> queryResultsCache;
    private final QueryForwarderManager queryForwarderManager;
    private final DatabaseComponent db;
    private final EventBus eventBus;

    @Inject
    public QueryReceiver(@QueryCache Cache<String, String> queryCache,
                         @QueryResultsCache Cache<String, QueryResponse> queryResultsCache,
                         SearchManager searchManager, QueryForwarderManager queryForwarderManager,
                         DatabaseComponent db, EventBus eventBus) {
        this.queryCache = queryCache;
        this.queryResultsCache = queryResultsCache;
        this.searchManager = searchManager;
        this.queryForwarderManager = queryForwarderManager;
        this.db = db;
        this.eventBus = eventBus;
    }

    @Override
    public void receiveMessage(@NotNull HeliosNetworkAddress heliosNetworkAddress, @NotNull String protocolId, @NotNull FileDescriptor fileDescriptor) {
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
    public void receiveMessage(@NotNull HeliosNetworkAddress heliosNetworkAddress, @NotNull String protocolId, @NotNull byte[] data) {
        String stringMessage = new String(data, StandardCharsets.UTF_8);
        LOG.info("Query Receiver: " + stringMessage);
        if (protocolId.equals(TEXT_QUERY_PROTOCOL)) {
            TextQuery textQuery = new Gson().fromJson(stringMessage, TextQuery.class);
            if (queryCache.asMap().containsKey(textQuery.getQueryId())) {
                LOG.info("Peer has seen the query already, discard query");
                return;
            }
            try {
                Transaction txn = db.startTransaction(false);
                try {
                    queryCache.put(textQuery.getQueryId(), heliosNetworkAddress.getNetworkId());
                    Map<Queryable, Double> results = searchManager.execute(txn, textQuery);

                    if (textQuery.isDead()) return;
                    if (results != null && results.size() > 0) {
                        QueryResponse qr = new QueryResponse(textQuery.getQueryId());
                        qr.appendResults(results);
                        if (queryResultsCache.asMap().containsKey(textQuery.getQueryId())) {
                            queryResultsCache.getIfPresent(textQuery.getQueryId())
                                    .appendEntities(qr.getEntities())
                                    .appendScores(qr.getScores());
                        } else {
                            queryResultsCache.put(textQuery.getQueryId(), qr);
                        }
                        eventBus.broadcast(
                                new QueryResponseReadyToSendEvent(
                                        new PeerId(heliosNetworkAddress.getNetworkId()),
                                        queryResultsCache.getIfPresent(textQuery.getQueryId()))
                        );
                        LOG.info(String.format("response to node: -->  %s : response %s", heliosNetworkAddress.getNetworkAddress(), qr.toString()));

                    } else {
                        LOG.info("No results for query " + textQuery.getQueryId() + ":" + textQuery.getQuery());
                    }

                    textQuery.decrementTLL();
                    if (textQuery.isDead()) {
                        LOG.info("Query is died discard " + textQuery.getQueryId() + ":" + textQuery.getQuery());
                    } else {
                        List<PeerId> forwardList = queryForwarderManager.getNextHops(new PeerId(heliosNetworkAddress.getNetworkId()), textQuery);
                        eventBus.broadcast(new ForwardQueryEvent(textQuery, forwardList));
                    }
                } catch (FormatException ex) {
                    ex.printStackTrace();
                } finally {
                    db.endTransaction(txn);
                }
            } catch (DbException e) {
                e.printStackTrace();
            }
        } else {
            LocationQuery locationQuery = new Gson().fromJson(stringMessage, LocationQuery.class);
            if (queryCache.asMap().containsKey(locationQuery.getQueryId())) {
                System.out.println("query cache: " + queryCache.asMap());
                LOG.info("Peer has seen the query already, discard query");
                return;
            }
            try {
                Transaction txn = db.startTransaction(false);
                try {
                    queryCache.put(locationQuery.getQueryId(), heliosNetworkAddress.getNetworkId());
                    Map<Queryable, Double> results = searchManager.execute(txn, locationQuery);

                    if (locationQuery.isDead()) return;
                    if (results != null && results.size() > 0) {
                        QueryResponse qr = new QueryResponse(locationQuery.getQueryId());
                        qr.appendResults(results);
                        if (queryResultsCache.asMap().containsKey(locationQuery.getQueryId())) {
                            queryResultsCache.getIfPresent(locationQuery.getQueryId())
                                    .appendEntities(qr.getEntities())
                                    .appendScores(qr.getScores());
                        } else {
                            queryResultsCache.put(locationQuery.getQueryId(), qr);
                        }
                        eventBus.broadcast(
                                new QueryResponseReadyToSendEvent(
                                        new PeerId(heliosNetworkAddress.getNetworkId()),
                                        queryResultsCache.getIfPresent(locationQuery.getQueryId()))
                        );
                        LOG.info(String.format("response to node: -->  %s : response %s", heliosNetworkAddress.getNetworkAddress(), qr.toString()));

                    } else {
                        LOG.info("No results for query " + locationQuery.getQueryId() + ":" + locationQuery.getQueryLatitude() + ", " + locationQuery.getQueryLongitude());
                    }

                    locationQuery.decrementTLL();
                    if (locationQuery.isDead()) {
                        LOG.info("Query is died discard " + locationQuery.getQueryId() + ":" + locationQuery.getQueryLatitude() + ", " + locationQuery.getQueryLongitude());
                    } else {
                        List<PeerId> forwardList = queryForwarderManager.getNextHops(new PeerId(heliosNetworkAddress.getNetworkId()), locationQuery);
                        eventBus.broadcast(new ForwardQueryEvent(locationQuery, forwardList));
                    }
                } catch (FormatException ex) {
                    ex.printStackTrace();
                } finally {
                    db.endTransaction(txn);
                }
            } catch (DbException e) {
                e.printStackTrace();
            }
        }
    }
}
