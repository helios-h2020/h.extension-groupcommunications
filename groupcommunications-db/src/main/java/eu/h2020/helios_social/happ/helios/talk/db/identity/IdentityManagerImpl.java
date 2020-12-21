package eu.h2020.helios_social.happ.helios.talk.db.identity;

import eu.h2020.helios_social.happ.helios.talk.api.crypto.CryptoComponent;
import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications.api.context.ContextType;
import eu.h2020.helios_social.modules.groupcommunications.api.context.DBContext;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.happ.helios.talk.api.identity.Identity;
import eu.h2020.helios_social.happ.helios.talk.api.identity.IdentityManager;
import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;
import eu.h2020.helios_social.happ.helios.talk.api.system.Clock;

import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import static eu.h2020.helios_social.happ.helios.talk.api.contact.HeliosLinkConstants.LINK_PREFIX;
import static java.util.logging.Logger.getLogger;
import static eu.h2020.helios_social.happ.helios.talk.api.util.LogUtils.logDuration;
import static eu.h2020.helios_social.happ.helios.talk.api.util.LogUtils.now;

@ThreadSafe
@NotNullByDefault
class IdentityManagerImpl implements IdentityManager,
		LifecycleManager.OpenDatabaseHook {

	private static final Logger LOG =
			getLogger(IdentityManagerImpl.class.getName());

	private final DatabaseComponent db;
	private final CryptoComponent crypto;
	private final Clock clock;

	/**
	 * The user's identity, or null if no identity has been registered or
	 * loaded. If non-null, this identity always has handshake keys.
	 */
	@Nullable
	private volatile Identity cachedIdentity = null;

	/**
	 * True if {@code cachedIdentity} was registered via
	 * {@link #registerIdentity(Identity)} and should be stored when
	 * {@link #onDatabaseOpened(Transaction)} is called.
	 */

	private volatile boolean shouldStoreIdentity = false;

	@Inject
	IdentityManagerImpl(DatabaseComponent db, CryptoComponent crypto,
			Clock clock) {
		this.db = db;
		this.crypto = crypto;
		this.clock = clock;
	}

	@Override
	public Identity createIdentity(String peerId, String name) {
		long start = now();
		logDuration(LOG, "Creating identity", start);
		return new Identity(peerId, name,
				clock.currentTimeMillis());
	}

	@Override
	public void registerIdentity(Identity i) {
		if (i.getId() == null) throw new IllegalArgumentException();
		cachedIdentity = i;
		shouldStoreIdentity = true;
		LOG.info("Identity registered");
	}

	@Override
	public void onDatabaseOpened(Transaction txn) throws DbException {
		Identity cached = getCachedIdentity(txn);
		if (shouldStoreIdentity) {
			// The identity was registered at startup - store it
			db.addIdentity(txn, cached);
			LOG.info("Identity stored");
			db.addContext(txn,
					new DBContext("All", "All", Integer.parseInt("222E3C", 16),
							ContextType.GENERAL));
			LOG.info("Default context stored");
		}
	}

	/**
	 * Loads the identity if necessary and returns it. If
	 * {@code cachedIdentity} was not already set by calling
	 * {@link #registerIdentity(Identity)}, this method sets it. If
	 * {@code cachedIdentity} was already set, either by calling
	 * {@link #registerIdentity(Identity)} or by a previous call to this
	 * method, then this method returns the cached identity without hitting
	 * the database.
	 */
	private Identity getCachedIdentity(Transaction txn) throws DbException {
		Identity cached = cachedIdentity;
		if (cached == null)
			cachedIdentity = cached = loadIdentity(txn);
		return cached;
	}

	/**
	 * Loads and returns the identity, generating a handshake key pair if
	 * necessary and setting {@code shouldStoreKeys} if a handshake key pair
	 * was generated.
	 */
	private Identity loadIdentity(Transaction txn)
			throws DbException {
		Identity identity = db.getIdentity(txn);
		LOG.info("Identity loaded");
		return identity;
	}

	@Override
	public Identity getIdentity() {
		Identity i = cachedIdentity;
		if (i == null) {
			try {
				i = db.transactionWithResult(true, this::getCachedIdentity);
			} catch (DbException e) {
				e.printStackTrace();
			}
		}
		return i;
	}

	@Override
	public void setNetworkId(String networkId) throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.setIdentityNetworkId(txn, networkId);
			if (cachedIdentity != null) cachedIdentity.setNetworkId(networkId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void setProfilePicture(byte[] profilePicture) throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.setIdentityProfilePicture(txn, profilePicture);
			if (cachedIdentity != null)
				cachedIdentity.setProfilePicture(profilePicture);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public String getHeliosLink() {
		Identity i = getIdentity();
		return LINK_PREFIX + i.getNetworkId();
	}
}
