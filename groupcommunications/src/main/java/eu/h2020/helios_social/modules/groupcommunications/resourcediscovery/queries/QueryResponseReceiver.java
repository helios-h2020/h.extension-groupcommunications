package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries;

import com.google.common.cache.Cache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

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
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryCache;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryResponse;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryResultsCache;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.Queryable;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryableDeserializer;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.QueryResponseReadyToSendEvent;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.QueryResultsReceivedEvent;

public class QueryResponseReceiver implements HeliosMessagingReceiver {
    private static Logger LOG = Logger.getLogger(QueryResponseReceiver.class.getName());

    private final QueryableDeserializer queryableDeserializer;
    private final Cache<String, String> queryCache;
    private final Cache<String, QueryResponse> queryResultsCache;
    private final EventBus eventBus;

    @Inject
    public QueryResponseReceiver(QueryableDeserializer queryableDeserializer,
                                 @QueryCache Cache<String, String> queryCache,
                                 @QueryResultsCache Cache<String, QueryResponse> queryResultsCache,
                                 EventBus eventBus) {
        this.queryableDeserializer = queryableDeserializer;
        this.queryCache = queryCache;
        this.queryResultsCache = queryResultsCache;
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

        LOG.info("Query Response Received: " + stringMessage);

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Queryable.class, queryableDeserializer)
                .create();

        QueryResponse<Queryable> queryResponse = gson.fromJson(stringMessage, new TypeToken<QueryResponse<Queryable>>() {
        }.getType());

        if (queryResponse.isDead())
        {   LOG.info("isDead");
            return;
        }

        if (queryCache.getIfPresent(queryResponse.getQueryId()) == null) return;
        String sourcePeer = queryCache.getIfPresent(queryResponse.getQueryId());

        if (queryResultsCache.getIfPresent(queryResponse.getQueryId()) != null) {
            try {
                queryResultsCache.getIfPresent(queryResponse.getQueryId())
                        .appendScores(queryResponse.getScores())
                        .appendEntities(queryResponse.getEntities());
            } catch (NullPointerException e) {
            }
        } else {
            queryResultsCache.put(queryResponse.getQueryId(), queryResponse);
        }


        QueryResponse qr = queryResultsCache.getIfPresent(queryResponse.getQueryId());

        LOG.info("Query Response is: " + qr + " sourcePeer: " + sourcePeer);
        qr.decrementTLL();
        if (qr.isDead()) {
            LOG.info("Response query is died discard " + qr.getQueryId());
        } else if (qr != null && sourcePeer != null && sourcePeer.equals("self")) {
            eventBus.broadcast(new QueryResultsReceivedEvent(qr));
        } else if (qr != null && sourcePeer != null) {
            eventBus.broadcast(new QueryResponseReadyToSendEvent(new PeerId(sourcePeer), qr));
        }

    }
}
