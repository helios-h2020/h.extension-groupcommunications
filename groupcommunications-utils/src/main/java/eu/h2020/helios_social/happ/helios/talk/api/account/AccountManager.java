package eu.h2020.helios_social.happ.helios.talk.api.account;

import javax.annotation.Nullable;

import eu.h2020.helios_social.happ.helios.talk.api.crypto.DecryptionException;
import eu.h2020.helios_social.happ.helios.talk.api.crypto.SecretKey;
import eu.h2020.helios_social.happ.helios.talk.api.identity.IdentityManager;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface AccountManager {

	/**
	 * Returns true if the manager has the database key. This will be false
	 * before {@link #createAccount(String, String)} or {@link #signIn(String)}
	 * has been called, and true after {@link #createAccount(String, String)}
	 * or {@link #signIn(String)} has returned true, until
	 * {@link #deleteAccount()} is called or the process exits.
	 */
	boolean hasDatabaseKey();

	/**
	 * Returns the database key if the manager has it. This will be null
	 * before {@link #createAccount(String, String)} or {@link #signIn(String)}
	 * has been called, and non-null after
	 * {@link #createAccount(String, String)} or {@link #signIn(String)} has
	 * returned true, until {@link #deleteAccount()} is called or the process
	 * exits.
	 */
	@Nullable
	SecretKey getDatabaseKey();

	/**
	 * Returns true if the encrypted database key can be loaded from disk.
	 */
	boolean accountExists();

	/**
	 * Creates an identity with the given name and registers it with the
	 * {@link IdentityManager}. Creates a database key, encrypts it with the
	 * given password and stores it on disk. {@link #accountExists()} will
	 * return true after this method returns true.
	 */
	boolean createAccount(String name, String password);

	/**
	 * Deletes all account state from disk. {@link #accountExists()} will
	 * return false after this method returns.
	 */
	void deleteAccount();

	/**
	 * Loads the encrypted database key from disk and decrypts it with the
	 * given password.
	 *
	 * @throws DecryptionException If the database key could not be loaded and
	 * decrypted.
	 */
	void signIn(String password) throws DecryptionException;

	/**
	 * Loads the encrypted database key from disk, decrypts it with the old
	 * password, encrypts it with the new password, and stores it on disk,
	 * replacing the old key.
	 *
	 * @throws DecryptionException If the database key could not be loaded and
	 * decrypted.
	 */
	void changePassword(String oldPassword, String newPassword)
			throws DecryptionException;
}
