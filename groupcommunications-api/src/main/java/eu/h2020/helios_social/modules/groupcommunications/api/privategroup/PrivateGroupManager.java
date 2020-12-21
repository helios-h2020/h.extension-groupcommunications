package eu.h2020.helios_social.modules.groupcommunications.api.privategroup;

import java.util.Collection;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.Contact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;

/**
 * Interface for SecretForumManager
 */
public interface PrivateGroupManager<T> {

    /**
     * Allows User to create a new Private Group with a given name
     *
     * @param privateGroup
     * @return
     */
    void addPrivateGroup(PrivateGroup privateGroup)
            throws DbException, FormatException;

    /**
     * Leaves a private group that was previously joined
     *
     * @param privateGroup
     */
    void leavePrivateGroup(PrivateGroup privateGroup);

    /**
     * @param gid group unique Identifier
     * @return the private group with the given id
     */
    PrivateGroup getPrivateGroup(String gid)
            throws FormatException, DbException;

    /**
     * @param contextId
     * @return all private groups of conversation in the given context
     */
    Collection<PrivateGroup> getPrivateGroups(String contextId)
            throws DbException, FormatException;

	Collection<PrivateGroup> getPrivateGroups()
			throws DbException, FormatException;

    Collection<PrivateGroup> getPrivateGroups(T txn)
            throws DbException, FormatException;

    /**
     * Returns all contact members of the given Private Group
     *
     * @param groupId
     * @return s collection of Private Group Members
     */
    Collection<Contact> getMembers(String groupId)
            throws DbException, FormatException;

    Collection<Contact> getMembers(T txn, String groupId)
            throws DbException, FormatException;

    void addMember(String groupId, ContactId contactId)
            throws DbException, FormatException;

    void addMember(T txn, String groupId, ContactId contactId)
            throws DbException, FormatException;

    void addPrivateGroup(T txn,
            PrivateGroup privateGroup)
            throws FormatException, DbException;
}
