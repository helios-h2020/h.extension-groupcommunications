package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.nlp;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.nlp.TermFrequency;

public class NormalizedTermFrequency implements TermFrequency {

	@Inject
	public NormalizedTermFrequency(){}

	public double calc(int termCount) {
		return Math.log(1D + termCount);
	}
}
