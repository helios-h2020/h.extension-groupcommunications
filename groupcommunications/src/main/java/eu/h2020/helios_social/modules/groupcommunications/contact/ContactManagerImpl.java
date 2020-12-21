package eu.h2020.helios_social.modules.groupcommunications.contact;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.inject.Inject;

import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.Contact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContact;

public class ContactManagerImpl implements ContactManager {

	private final DatabaseComponent db;

	@Inject
	public ContactManagerImpl(DatabaseComponent db) {
		this.db = db;
	}

	@Override
	public void addContact(Contact contact) throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.addContact(txn, contact);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void addPendingContact(PendingContact pendingContact)
			throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.addPendingContact(txn, pendingContact);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public PendingContact getPendingContact(ContactId pendingContactId)
			throws DbException {
		Transaction txn = db.startTransaction(true);
		PendingContact pendingContact = null;
		try {
			pendingContact = db.getPendingContact(txn, pendingContactId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return pendingContact;
	}

	@Override
	public Contact getContact(ContactId contactId)
			throws DbException {
		Transaction txn = db.startTransaction(true);
		Contact contact = null;
		try {
			contact = db.getContact(txn, contactId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return contact;
	}

	@Override
	public void deleteContact(ContactId contactId) throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.removeContact(txn, contactId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void deletePendingContact(ContactId contactId) throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.removePendingContact(txn, contactId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void deleteAllContacts() {
		//TODO
	}

	@Override
	public Collection<Contact> getContacts() throws DbException {
		Transaction txn = db.startTransaction(true);
		Collection<Contact> contacts = null;
		try {
			contacts = db.getContacts(txn);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return contacts;
	}

	@Override
	public Collection<Contact> getContacts(
			String contextId) throws DbException {
		Transaction txn = db.startTransaction(true);
		Collection<Contact> filteredContacts = null;
		try {
			Collection<Contact> contacts = db.getContacts(txn);
			Collection<String> contextContacts =
					db.getContactIds(txn, contextId);
			filteredContacts = contacts.stream().filter(c -> {
				return contextContacts.contains(c.getId().getId());
			}).collect(
					Collectors.toList());
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return filteredContacts;
	}

	@Override
	public Collection<PendingContact> getPendingContacts() throws DbException {
		Transaction txn = db.startTransaction(true);
		Collection<PendingContact> pendingContacts = null;
		try {
			pendingContacts = db.getPendingContacts(txn);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return pendingContacts;
	}
}
