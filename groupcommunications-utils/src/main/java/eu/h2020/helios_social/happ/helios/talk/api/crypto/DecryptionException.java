package eu.h2020.helios_social.happ.helios.talk.api.crypto;

import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public class DecryptionException extends Exception {

	private final DecryptionResult result;

	public DecryptionException(DecryptionResult result) {
		this.result = result;
	}

	public DecryptionResult getDecryptionResult() {
		return result;
	}
}
