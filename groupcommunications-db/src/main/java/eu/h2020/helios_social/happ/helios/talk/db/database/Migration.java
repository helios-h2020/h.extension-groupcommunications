package eu.h2020.helios_social.happ.helios.talk.db.database;

import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;

interface Migration<T> {

	/**
	 * Returns the schema version from which this migration starts.
	 */
	int getStartVersion();

	/**
	 * Returns the schema version at which this migration ends.
	 */
	int getEndVersion();

	void migrate(T txn) throws DbException;
}
