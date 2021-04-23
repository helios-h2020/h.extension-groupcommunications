package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.index;

import android.app.Application;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.R;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.EntityType;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.index.Indexer;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.index.InvertedIndexManager;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.nlp.Stemmer;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.nlp.TermFrequency;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.nlp.Tokenizer;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.Queryable;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;

public class ForumIndexer implements Indexer<Transaction> {

    private final InvertedIndexManager invertedIndexManager;
    private final Tokenizer tokenizer;
    private final Stemmer stemmer;
    private final TermFrequency tf;
    private List<String> stopWords;

    @Inject
    public ForumIndexer(InvertedIndexManager invertedIndexManager,
                        Tokenizer tokenizer, Stemmer stemmer, TermFrequency tf) {
        this.invertedIndexManager = invertedIndexManager;
        this.tokenizer = tokenizer;
        this.stemmer = stemmer;
        this.tf = tf;
        this.stopWords = new ArrayList();
    }

    @Override
    public void addQueryable(Transaction txn, Queryable queryable) throws DbException, FormatException {
        Forum forum = (Forum) queryable;
        List<String> terms = getListOfTerms(getForumTextRepresentation(forum));
        invertedIndexManager.addTerms(txn, EntityType.FORUM, forum.getId(), forum.getContextId(), terms);
    }

    @Override
    public Map<String, Double> search(Transaction txn, String text, String contextId) throws DbException, FormatException {
        Set<String> textTerms = new HashSet<>(getListOfTerms(text));

        Map<String, Double> score = new HashMap<>();

        for (String term : textTerms) {
            for (Object doc : invertedIndexManager.getEntities(txn, EntityType.FORUM, contextId, term)) {
                score.putIfAbsent((String) doc, 0.0);

                double termScore = tf.calc(invertedIndexManager.getTermCount(txn, EntityType.FORUM, (String) doc, contextId, term));
                score.put((String) doc, score.get(doc) + termScore);
            }
        }

        return score.entrySet().stream().sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private List<String> getListOfTerms(String text) {
        List<String> terms = tokenizer.tokenize(text);
        return terms.stream().filter(token -> !stopWords.contains(token)).map(stemmer::stem)
                .collect(Collectors.toList());
    }

    private String getForumTextRepresentation(Forum f) {
        String representation = f.getName() + " " + String.join(" ", f.getTags());
        return representation.toLowerCase();
    }
}
