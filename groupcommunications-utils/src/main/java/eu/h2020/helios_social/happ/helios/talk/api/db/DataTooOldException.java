package eu.h2020.helios_social.happ.helios.talk.api.db;

import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;

/**
 * Thrown when the database uses an older schema than the current code and
 * cannot be migrated.
 */
public class DataTooOldException extends DbException {
}
