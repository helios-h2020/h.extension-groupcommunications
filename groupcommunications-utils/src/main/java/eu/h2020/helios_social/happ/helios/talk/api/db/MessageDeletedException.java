package eu.h2020.helios_social.happ.helios.talk.api.db;

import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;

/**
 * Thrown when a message that has been deleted is requested from the database.
 * This exception may occur due to concurrent updates and does not indicate a
 * database error.
 */
public class MessageDeletedException extends DbException {
}
