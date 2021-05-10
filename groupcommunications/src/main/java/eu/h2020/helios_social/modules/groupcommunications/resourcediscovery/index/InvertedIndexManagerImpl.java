package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.index;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.EntityType;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.index.InvertedIndexManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfDictionary;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.Encoder;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.Parser;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Metadata;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;

public class InvertedIndexManagerImpl implements InvertedIndexManager<Transaction> {
    private static Logger LOG = Logger.getLogger(InvertedIndexManager.class.getName());

    private final DatabaseComponent db;
    private final Encoder encoder;
    private final Parser parser;

    @Inject
    public InvertedIndexManagerImpl(DatabaseComponent db, Encoder encoder, Parser parser) {
        this.db = db;
        this.encoder = encoder;
        this.parser = parser;
    }

    @Override
    public void addTerms(Transaction txn, EntityType entityType, String entityId, String contextId, List<String> terms) throws DbException, FormatException {
        Metadata metadata = db.getInvertedIndexMetadata(txn, entityType, contextId);
        BdfDictionary meta = parser.parseMetadata(metadata);
        for (String term : terms) {
            BdfDictionary dictionary = meta.getDictionary(term, new BdfDictionary());
            dictionary.put(entityId, dictionary.getInteger(entityId, 0) + 1);
            meta.put(term, dictionary);
        }
        LOG.info("Terms: " + terms);
        LOG.info("METADATA: " + meta);
        db.mergeInvertedIndexMetadata(txn, entityType, contextId, encoder.encodeMetadata(meta));
    }

    @Override
    public Set<String> getVocabulary(Transaction txn, EntityType entityType, String contextId) throws DbException, FormatException {
        Metadata metadata = db.getInvertedIndexMetadata(txn, entityType, contextId);
        BdfDictionary meta = parser.parseMetadata(metadata);
        return meta.keySet();
    }

    @Override
    public Set<String> getEntities(Transaction txn, EntityType entityType, String contextId, String term) throws DbException, FormatException {
        Metadata metadata = db.getInvertedIndexMetadata(txn, entityType, contextId);
        LOG.info("METADATA: " + metadata);
        LOG.info("METADATA: " + metadata.isEmpty());
        if (metadata.isEmpty()) return new HashSet();
        BdfDictionary meta = parser.parseMetadata(metadata);
        return meta.getDictionary(term, new BdfDictionary()).keySet();
    }

    @Override
    public Integer getTermCount(Transaction txn, EntityType entityType, String entityId, String contextId, String term) throws DbException, FormatException {
        Metadata metadata = db.getInvertedIndexMetadata(txn, entityType, contextId);
        if (metadata.isEmpty()) return 0;
        BdfDictionary meta = parser.parseMetadata(metadata);
        return meta.getDictionary(term, new BdfDictionary()).getInteger(entityId, 0);
    }

    @Override
    public void removeEntity(Transaction txn, EntityType entityType, String entityId, String contextId) throws FormatException, DbException {
        Metadata metadata = db.getInvertedIndexMetadata(txn, entityType, contextId);
        BdfDictionary meta = parser.parseMetadata(metadata);
        for (String key : meta.keySet()) {
            BdfDictionary dictionary = meta.getDictionary(key);
            dictionary.remove(entityId);
            meta.put(key, dictionary);
        }
        db.mergeInvertedIndexMetadata(txn, entityType, contextId, encoder.encodeMetadata(meta));
    }


}
