package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.nlp;


import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.nlp.TermFrequency;

public class RawTermFrequency implements TermFrequency {

	public double calc(int termCount) {
		return (double) termCount;
	}
}
