package eu.h2020.helios_social.modules.groupcommunications.api.contact;

import java.util.Collection;

import eu.h2020.helios_social.core.contextualegonetwork.Context;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;

public interface ContactManager {

	public void addContact(Contact contact) throws DbException;

	public void addPendingContact(PendingContact pendingContact)
			throws DbException;

	PendingContact getPendingContact(ContactId pendingContactId)
			throws DbException;

	Contact getContact(ContactId contactId)
			throws DbException;

	public void deleteContact(ContactId contactId) throws DbException;

	void deletePendingContact(ContactId contactId) throws DbException;

	public void deleteAllContacts();

	Collection<Contact> getContacts() throws DbException;

	Collection<Contact> getContacts(
			String contextId) throws DbException;

	public Collection<PendingContact> getPendingContacts() throws DbException;

}
