package eu.h2020.helios_social.happ.helios.talk.api.crypto;

import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;

/**
 * The public half of a public/private {@link KeyPair}.
 */
@NotNullByDefault
public interface PublicKey {

	/**
	 * Returns the type of this key pair.
	 */
	String getKeyType();

	/**
	 * Returns the encoded representation of this key.
	 */
	byte[] getEncoded();
}
