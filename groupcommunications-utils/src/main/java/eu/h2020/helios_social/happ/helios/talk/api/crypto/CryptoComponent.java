package eu.h2020.helios_social.happ.helios.talk.api.crypto;

import java.security.SecureRandom;

import javax.annotation.Nullable;

import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface CryptoComponent {

	SecretKey generateSecretKey();

	SecureRandom getSecureRandom();

	// Package access for testing
	/*byte[] performRawKeyAgreement(PrivateKey priv, PublicKey pub)
			throws GeneralSecurityException {
		if (!priv.getKeyType().equals(KEY_TYPE_AGREEMENT))
			throw new IllegalArgumentException();
		if (!pub.getKeyType().equals(KEY_TYPE_AGREEMENT))
			throw new IllegalArgumentException();
		long start = now();
		byte[] secret = curve25519.calculateAgreement(pub.getEncoded(),
				priv.getEncoded());
		// If the shared secret is all zeroes, the public key is invalid
		byte allZero = 0;
		for (byte b : secret) allZero |= b;
		if (allZero == 0) throw new GeneralSecurityException();
		logDuration(LOG, "Deriving shared secret", start);
		return secret;
	}

	@Override
	public KeyPair generateAgreementKeyPair() {
		Curve25519KeyPair keyPair = curve25519.generateKeyPair();
		PublicKey pub = new AgreementPublicKey(keyPair.getPublicKey());
		PrivateKey priv = new AgreementPrivateKey(keyPair.getPrivateKey());
		return new KeyPair(pub, priv);
	}

	@Override
	public KeyParser getAgreementKeyParser() {
		return agreementKeyParser;
	}

	@Override
	public KeyPair generateSignatureKeyPair() {
		java.security.KeyPair keyPair =
				signatureKeyPairGenerator.generateKeyPair();
		EdDSAPublicKey edPublicKey = (EdDSAPublicKey) keyPair.getPublic();
		PublicKey publicKey = new SignaturePublicKey(edPublicKey.getAbyte());
		EdDSAPrivateKey edPrivateKey = (EdDSAPrivateKey) keyPair.getPrivate();
		PrivateKey privateKey = new SignaturePrivateKey(edPrivateKey.getSeed());
		return new KeyPair(publicKey, privateKey);
	}

	@Override
	public KeyParser getSignatureKeyParser() {
		return signatureKeyParser;
	}

	@Override
	public KeyParser getMessageKeyParser() {
		return messageEncrypter.getKeyParser();
	}*/
	/*SecretKey deriveKey(String label, SecretKey k, byte[]... inputs);

	/**
	 * Returns the hash of the given inputs. The inputs are unambiguously
	 * combined by prefixing each input with its length.
	 *
	 * @param label A namespaced label indicating the purpose of this hash, to
	 * prevent it from being repurposed or colliding with a hash created for
	 * another purpose
	 */
	/*byte[] hash(String label, byte[]... inputs);

	/**
	 * Returns a message authentication code with the given key over the
	 * given inputs. The inputs are unambiguously combined by prefixing each
	 * input with its length.
	 *
	 * @param label A namespaced label indicating the purpose of this MAC, to
	 * prevent it from being repurposed or colliding with a MAC created for
	 * another purpose
	 */
	/*byte[] mac(String label, SecretKey macKey, byte[]... inputs);

	/**
	 * Verifies that the given message authentication code is valid for the
	 * given secret key and inputs.
	 *
	 * @param label A namespaced label indicating the purpose of this MAC, to
	 * prevent it from being repurposed or colliding with a MAC created for
	 * another purpose
	 * @return True if the MAC was valid, false otherwise.
	 */
	/*boolean verifyMac(byte[] mac, String label, SecretKey macKey,
			byte[]... inputs);

	/**
	 * Encrypts and authenticates the given plaintext so it can be written to
	 * storage. The encryption and authentication keys are derived from the
	 * given password. The ciphertext will be decryptable using the same
	 * password after the app restarts.
	 *
	 * @param keyStrengthener Used to strengthen the password-based key. If
	 * null, the password-based key will not be strengthened
	 */
	byte[] encryptWithPassword(byte[] plaintext, String password,
			@Nullable KeyStrengthener keyStrengthener);

	/**
	 * Decrypts and authenticates the given ciphertext that has been read from
	 * storage. The encryption and authentication keys are derived from the
	 * given password.
	 *
	 * @param keyStrengthener Used to strengthen the password-based key. If
	 * null, or if strengthening was not used when encrypting the ciphertext,
	 * the password-based key will not be strengthened
	 * @throws DecryptionException If the ciphertext cannot be decrypted and
	 * authenticated (for example, if the password is wrong).
	 */
	byte[] decryptWithPassword(byte[] ciphertext, String password,
			@Nullable KeyStrengthener keyStrengthener)
			throws DecryptionException;

	/**
	 * Returns true if the given ciphertext was encrypted using a strengthened
	 * key. The validity of the ciphertext is not checked.
	 */
	boolean isEncryptedWithStrengthenedKey(byte[] ciphertext);
}
