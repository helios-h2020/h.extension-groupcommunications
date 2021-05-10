package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.nlp.Tokenizer;

public class SimpleTokenizer implements Tokenizer {

    @Inject
    public SimpleTokenizer() {
    }

    @Override
    public List<String> tokenize(String str) {
        int firstChar = -1;
        for (int i = 0; i < str.length(); i++) {
            if (Character.isLetter(str.charAt(i))) {
                firstChar = i;
                break;
            }
        }
        if (firstChar == -1) {
            return new ArrayList<String>();
        }

        return Arrays.asList(str.substring(firstChar).toLowerCase().split("[^a-z]+"));
    }
}
