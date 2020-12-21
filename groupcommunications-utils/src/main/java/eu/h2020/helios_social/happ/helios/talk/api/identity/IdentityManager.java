package eu.h2020.helios_social.happ.helios.talk.api.identity;

import eu.h2020.helios_social.happ.helios.talk.api.crypto.CryptoExecutor;
import eu.h2020.helios_social.happ.helios.talk.api.crypto.SecretKey;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface IdentityManager {

	/**
	 * Creates an identity with the given name.
	 */
	@CryptoExecutor
	Identity createIdentity(String peerId, String name);

	/**
	 * Registers the given identity with the manager. This method should be
	 * called before {@link LifecycleManager#startServices(SecretKey)}. The
	 * identity is stored when {@link LifecycleManager#startServices(SecretKey)}
	 * is called. The identity must include a handshake key pair.
	 */
	void registerIdentity(Identity i);

	Identity getIdentity();

	void setNetworkId(String networkId) throws DbException;

	void setProfilePicture(byte[] profilePicture) throws DbException;

	String getHeliosLink();
}
