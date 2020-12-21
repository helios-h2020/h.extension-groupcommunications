package eu.h2020.helios_social.happ.helios.talk.api.db;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;

/**
 * Thrown when a duplicate contact is added to the database. This exception may
 * occur due to concurrent updates and does not indicate a database error.
 */
public class ContactExistsException extends DbException {

	private final ContactId contactId;
	private final String alias;

	public ContactExistsException(ContactId cid, String alias) {
		this.contactId = cid;
		this.alias = alias;
	}

	public ContactId getContactId() {
		return contactId;
	}

	public String getAlias() {
		return alias;
	}
}
