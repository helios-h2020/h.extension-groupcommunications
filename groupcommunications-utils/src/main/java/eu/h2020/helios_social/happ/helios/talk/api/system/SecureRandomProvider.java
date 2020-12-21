package eu.h2020.helios_social.happ.helios.talk.api.system;

import java.security.Provider;
import java.security.SecureRandom;

import javax.annotation.Nullable;

import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;

/**
 * Wrapper for a platform-specific secure random number generator.
 */
@NotNullByDefault
public interface SecureRandomProvider {

	/**
	 * Returns a {@link Provider} that provides a strong {@link SecureRandom}
	 * implementation, or null if the platform's default implementation should
	 * be used.
	 */
	@Nullable
	Provider getProvider();
}
