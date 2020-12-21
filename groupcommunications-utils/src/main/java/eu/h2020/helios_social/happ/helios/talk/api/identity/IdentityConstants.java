package eu.h2020.helios_social.happ.helios.talk.api.identity;

import static eu.h2020.helios_social.happ.helios.talk.api.crypto.CryptoConstants.MAX_SIGNATURE_BYTES;
import static eu.h2020.helios_social.happ.helios.talk.api.crypto.CryptoConstants.MAX_SIGNATURE_PUBLIC_KEY_BYTES;

public interface IdentityConstants {

	/**
	 * The maximum length of an author's name in UTF-8 bytes.
	 */
	int MAX_IDENTITY_NAME_LENGTH = 50;
}
