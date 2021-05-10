package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery;

import androidx.room.Query;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.event.HeliosEvent;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.LocationForum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.SeasonalForum;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.index.Indexer;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.index.InvertedIndexManager;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.nlp.Stemmer;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.nlp.TermFrequency;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.nlp.Tokenizer;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryForwarderManager;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryManager;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.SearchManager;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryCache;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryResponse;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryResultsCache;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryableDeserializer;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.index.ForumIndexer;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.index.InvertedIndexManagerImpl;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.nlp.NormalizedTermFrequency;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.nlp.PortStemmer;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.nlp.SimpleTokenizer;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.QueryForwarderManagerImpl;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.QueryManagerImpl;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.SearchManagerImpl;
import eu.h2020.helios_social.modules.groupcommunications_utils.lifecycle.LifecycleManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;

@Module
public class ResourceDiscoveryModule {

    public static class EagerSingletons {
        @Inject
        QueryManager queryManager;
    }

    private final Cache<String, String> queryCache;
    private final Cache<String, QueryResponse> queryResultsCache;

    public ResourceDiscoveryModule() {
        queryCache = CacheBuilder.newBuilder().maximumSize(1000)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
        queryResultsCache = CacheBuilder.newBuilder().maximumSize(1000)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    Stemmer providesStemmer(
            PortStemmer portStemmer) {
        return portStemmer;
    }

    @Provides
    @Singleton
    Tokenizer providesTokenizer(
            SimpleTokenizer simpleTokenizer) {
        return simpleTokenizer;
    }

    @Provides
    @Singleton
    TermFrequency providesTermFrequency(
            NormalizedTermFrequency termFrequency) {
        return termFrequency;
    }

    @Provides
    @Singleton
    Indexer providesForumIndexer(ForumIndexer forumIndexer) {
        return forumIndexer;
    }

    @Provides
    @Singleton
    InvertedIndexManager providesInvertedIndexManager(InvertedIndexManagerImpl invertedIndexManager) {
        return invertedIndexManager;
    }

    @Provides
    @Singleton
    @QueryCache
    Cache<String, String> providesQueryCache() {
        return queryCache;
    }

    @Provides
    @Singleton
    @QueryResultsCache
    Cache<String, QueryResponse> providesQueryResultsCache() {
        return queryResultsCache;
    }

    @Provides
    @Singleton
    SearchManager providesSearchManager(SearchManagerImpl searchManager) {
        return searchManager;
    }

    @Provides
    @Singleton
    QueryForwarderManager providesQueryForwarderManager(QueryForwarderManagerImpl queryForwarderManager) {
        return queryForwarderManager;
    }

    @Provides
    @Singleton
    QueryManager providesQueryManager(EventBus eventBus, QueryManagerImpl queryManager) {
        eventBus.addListener(queryManager);
        return queryManager;
    }

    @Provides
    @Singleton
    QueryableDeserializer providesQueryableDeserializer() {
        QueryableDeserializer deserializer = new QueryableDeserializer("queryableType");
        deserializer.registerQueryableType("Forum", Forum.class);
        deserializer.registerQueryableType("LocationForum", LocationForum.class);
        deserializer.registerQueryableType("SeasonalForum", SeasonalForum.class);
        deserializer.registerQueryableType("HeliosEvent", HeliosEvent.class);
        return deserializer;
    }

}
