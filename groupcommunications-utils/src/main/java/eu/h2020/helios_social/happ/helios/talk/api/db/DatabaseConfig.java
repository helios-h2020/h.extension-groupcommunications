package eu.h2020.helios_social.happ.helios.talk.api.db;

import java.io.File;

import javax.annotation.Nullable;

import eu.h2020.helios_social.happ.helios.talk.api.crypto.KeyStrengthener;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface DatabaseConfig {

	/**
	 * Returns the directory where the database stores its data.
	 */
	File getDatabaseDirectory();

	/**
	 * Returns the directory where the encrypted database key is stored.
	 */
	File getDatabaseKeyDirectory();

	/**
	 * Returns a {@link KeyStrengthener} for strengthening the encryption of
	 * the database key, or null if no strengthener should be used.
	 */
	@Nullable
	KeyStrengthener getKeyStrengthener();
}
