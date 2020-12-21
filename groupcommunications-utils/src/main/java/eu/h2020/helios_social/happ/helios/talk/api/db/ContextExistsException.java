package eu.h2020.helios_social.happ.helios.talk.api.db;

import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;

/**
 * Thrown when a duplicate contact is added to the database. This exception may
 * occur due to concurrent updates and does not indicate a database error.
 */
public class ContextExistsException extends DbException {

	private final String contextId;

	public ContextExistsException(String contextId) {
		this.contextId = contextId;
	}

	public String getContextId() {
		return contextId;
	}

}

