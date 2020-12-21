package eu.h2020.helios_social.happ.helios.talk.db.crypto;

import eu.h2020.helios_social.happ.helios.talk.api.crypto.CryptoComponent;
import eu.h2020.helios_social.happ.helios.talk.api.crypto.DecryptionException;
import eu.h2020.helios_social.happ.helios.talk.api.crypto.KeyStrengthener;
import eu.h2020.helios_social.happ.helios.talk.api.crypto.SecretKey;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;
import eu.h2020.helios_social.happ.helios.talk.api.system.SecureRandomProvider;
import eu.h2020.helios_social.happ.helios.talk.api.util.ByteUtils;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static java.lang.System.arraycopy;
import static java.util.logging.Level.INFO;
import static eu.h2020.helios_social.happ.helios.talk.api.crypto.DecryptionResult.INVALID_CIPHERTEXT;
import static eu.h2020.helios_social.happ.helios.talk.api.crypto.DecryptionResult.INVALID_PASSWORD;
import static eu.h2020.helios_social.happ.helios.talk.api.crypto.DecryptionResult.KEY_STRENGTHENER_ERROR;
import static eu.h2020.helios_social.happ.helios.talk.api.util.ByteUtils.INT_32_BYTES;

@NotNullByDefault
class CryptoComponentImpl implements CryptoComponent {

	private static final Logger LOG =
			Logger.getLogger(CryptoComponentImpl.class.getName());
;
	private static final int STORAGE_IV_BYTES = 24; // 196 bits
	private static final int PBKDF_SALT_BYTES = 32; // 256 bits
	private static final byte PBKDF_FORMAT_SCRYPT = 0;
	private static final byte PBKDF_FORMAT_SCRYPT_STRENGTHENED = 1;

	private final SecureRandom secureRandom;
	private final PasswordBasedKdf passwordBasedKdf;

	@Inject
	CryptoComponentImpl(SecureRandomProvider secureRandomProvider,
			PasswordBasedKdf passwordBasedKdf) {
		if (LOG.isLoggable(INFO)) {
			SecureRandom defaultSecureRandom = new SecureRandom();
			String name = defaultSecureRandom.getProvider().getName();
			String algorithm = defaultSecureRandom.getAlgorithm();
			LOG.info("Default SecureRandom: " + name + " " + algorithm);
		}
		Provider provider = secureRandomProvider.getProvider();
		if (provider == null) {
			LOG.info("Using default");
		} else {
			installSecureRandomProvider(provider);
			if (LOG.isLoggable(INFO)) {
				SecureRandom installedSecureRandom = new SecureRandom();
				String name = installedSecureRandom.getProvider().getName();
				String algorithm = installedSecureRandom.getAlgorithm();
				LOG.info("Installed SecureRandom: " + name + " " + algorithm);
			}
		}
		secureRandom = new SecureRandom();
		this.passwordBasedKdf = passwordBasedKdf;
	}

	// Based on https://android-developers.googleblog.com/2013/08/some-securerandom-thoughts.html
	private void installSecureRandomProvider(Provider provider) {
		Provider[] providers = Security.getProviders("SecureRandom.SHA1PRNG");
		if (providers == null || providers.length == 0
				|| !provider.getClass().equals(providers[0].getClass())) {
			Security.insertProviderAt(provider, 1);
		}
		// Check the new provider is the default when no algorithm is specified
		SecureRandom random = new SecureRandom();
		if (!provider.getClass().equals(random.getProvider().getClass())) {
			throw new SecurityException("Wrong SecureRandom provider: "
					+ random.getProvider().getClass());
		}
		// Check the new provider is the default when SHA1PRNG is specified
		try {
			random = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			throw new SecurityException(e);
		}
		if (!provider.getClass().equals(random.getProvider().getClass())) {
			throw new SecurityException("Wrong SHA1PRNG provider: "
					+ random.getProvider().getClass());
		}
	}

	@Override
	public SecretKey generateSecretKey() {
		byte[] b = new byte[SecretKey.LENGTH];
		secureRandom.nextBytes(b);
		return new SecretKey(b);
	}

	@Override
	public SecureRandom getSecureRandom() {
		return secureRandom;
	}

	/*@Override
	public boolean verifyMac(byte[] mac, String label, SecretKey macKey,
			byte[]... inputs) {
		byte[] expected = mac(label, macKey, inputs);
		if (mac.length != expected.length) return false;
		// Constant-time comparison
		int cmp = 0;
		for (int i = 0; i < mac.length; i++) cmp |= mac[i] ^ expected[i];
		return cmp == 0;
	}*/

	@Override
	public byte[] encryptWithPassword(byte[] input, String password,
			@Nullable KeyStrengthener keyStrengthener) {
		AuthenticatedCipher cipher = new XSalsa20Poly1305AuthenticatedCipher();
		int macBytes = cipher.getMacBytes();
		// Generate a random salt
		byte[] salt = new byte[PBKDF_SALT_BYTES];
		secureRandom.nextBytes(salt);
		// Calibrate the KDF
		int cost = passwordBasedKdf.chooseCostParameter();
		// Derive the encryption key from the password
		SecretKey key = passwordBasedKdf.deriveKey(password, salt, cost);
		if (keyStrengthener != null) key = keyStrengthener.strengthenKey(key);
		// Generate a random IV
		byte[] iv = new byte[STORAGE_IV_BYTES];
		secureRandom.nextBytes(iv);
		// The output contains the format version, salt, cost parameter, IV,
		// ciphertext and MAC
		int outputLen = 1 + salt.length + INT_32_BYTES + iv.length
				+ input.length + macBytes;
		byte[] output = new byte[outputLen];
		int outputOff = 0;
		// Format version
		byte formatVersion = keyStrengthener == null
				? PBKDF_FORMAT_SCRYPT : PBKDF_FORMAT_SCRYPT_STRENGTHENED;
		output[outputOff] = formatVersion;
		outputOff++;
		// Salt
		arraycopy(salt, 0, output, outputOff, salt.length);
		outputOff += salt.length;
		// Cost parameter
		ByteUtils.writeUint32(cost, output, outputOff);
		outputOff += INT_32_BYTES;
		// IV
		arraycopy(iv, 0, output, outputOff, iv.length);
		outputOff += iv.length;
		// Initialise the cipher and encrypt the plaintext
		try {
			cipher.init(true, key, iv);
			cipher.process(input, 0, input.length, output, outputOff);
			return output;
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public byte[] decryptWithPassword(byte[] input, String password,
			@Nullable KeyStrengthener keyStrengthener)
			throws DecryptionException {
		AuthenticatedCipher cipher = new XSalsa20Poly1305AuthenticatedCipher();
		int macBytes = cipher.getMacBytes();
		// The input contains the format version, salt, cost parameter, IV,
		// ciphertext and MAC
		if (input.length < 1 + PBKDF_SALT_BYTES + INT_32_BYTES
				+ STORAGE_IV_BYTES + macBytes) {
			throw new DecryptionException(INVALID_CIPHERTEXT);
		}
		int inputOff = 0;
		// Format version
		byte formatVersion = input[inputOff];
		inputOff++;
		// Check whether we support this format version
		if (formatVersion != PBKDF_FORMAT_SCRYPT &&
				formatVersion != PBKDF_FORMAT_SCRYPT_STRENGTHENED) {
			throw new DecryptionException(INVALID_CIPHERTEXT);
		}
		// Salt
		byte[] salt = new byte[PBKDF_SALT_BYTES];
		arraycopy(input, inputOff, salt, 0, salt.length);
		inputOff += salt.length;
		// Cost parameter
		long cost = ByteUtils.readUint32(input, inputOff);
		inputOff += INT_32_BYTES;
		if (cost < 2 || cost > Integer.MAX_VALUE) {
			throw new DecryptionException(INVALID_CIPHERTEXT);
		}
		// IV
		byte[] iv = new byte[STORAGE_IV_BYTES];
		arraycopy(input, inputOff, iv, 0, iv.length);
		inputOff += iv.length;
		// Derive the decryption key from the password
		SecretKey key = passwordBasedKdf.deriveKey(password, salt, (int) cost);
		if (formatVersion == PBKDF_FORMAT_SCRYPT_STRENGTHENED) {
			if (keyStrengthener == null || !keyStrengthener.isInitialised()) {
				// Can't derive the same strengthened key
				throw new DecryptionException(KEY_STRENGTHENER_ERROR);
			}
			key = keyStrengthener.strengthenKey(key);
		}
		// Initialise the cipher
		try {
			cipher.init(false, key, iv);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
		// Try to decrypt the ciphertext (may be invalid)
		try {
			int inputLen = input.length - inputOff;
			byte[] output = new byte[inputLen - macBytes];
			cipher.process(input, inputOff, inputLen, output, 0);
			return output;
		} catch (GeneralSecurityException e) {
			throw new DecryptionException(INVALID_PASSWORD);
		}
	}

	@Override
	public boolean isEncryptedWithStrengthenedKey(byte[] ciphertext) {
		return ciphertext.length > 0 &&
				ciphertext[0] == PBKDF_FORMAT_SCRYPT_STRENGTHENED;
	}
}
