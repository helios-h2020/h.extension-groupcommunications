package eu.h2020.helios_social.happ.helios.talk.api.db;


import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContact;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;

/**
 * Thrown when a duplicate pending contact is added to the database. This
 * exception may occur due to concurrent updates and does not indicate a
 * database error.
 */
public class PendingContactExistsException extends DbException {

	private final PendingContact pendingContact;

	public PendingContactExistsException(PendingContact pendingContact) {
		this.pendingContact = pendingContact;
	}

	public PendingContact getPendingContact() {
		return pendingContact;
	}
}
