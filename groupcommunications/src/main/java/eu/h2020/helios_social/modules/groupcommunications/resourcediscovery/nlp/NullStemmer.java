package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.nlp;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.nlp.Stemmer;

public class NullStemmer implements Stemmer {

    public NullStemmer(){}

    // does not stem at all
    @Override
    public String stem(String word) {
        return word;
    }
}
