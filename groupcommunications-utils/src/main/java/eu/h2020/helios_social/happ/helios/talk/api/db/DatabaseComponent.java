package eu.h2020.helios_social.happ.helios.talk.api.db;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import eu.h2020.helios_social.happ.helios.talk.api.crypto.SecretKey;
import eu.h2020.helios_social.happ.helios.talk.api.identity.Identity;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;
import eu.h2020.helios_social.happ.helios.talk.api.settings.Settings;
import eu.h2020.helios_social.modules.groupcommunications.api.event.HeliosEvent;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMember;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMemberRole;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageState;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.Contact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContact;
import eu.h2020.helios_social.modules.groupcommunications.api.context.DBContext;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.Profile;


/**
 * Encapsulates the database implementation and exposes high-level operations
 * to other components.
 */
@NotNullByDefault
public interface DatabaseComponent extends TransactionManager {

	/**
	 * Opens the database and returns true if the database already existed.
	 *
	 * @throws DataTooNewException if the data uses a newer schema than the
	 *                             current code
	 * @throws DataTooOldException if the data uses an older schema than the
	 *                             current code and cannot be migrated
	 */
	boolean open(SecretKey key, @Nullable MigrationListener listener)
			throws DbException;

	/**
	 * Waits for any open transactions to finish and closes the database.
	 */
	void close() throws DbException;

	/**
	 * Stores an identity.
	 */
	void addIdentity(Transaction txn, Identity i) throws DbException;

	void addProfile(Transaction transaction, Profile profile)
			throws DbException;

	boolean containsIdentity(Transaction transaction)
			throws DbException;

	/**
	 * Returns the identity for the local pseudonym with the given ID.
	 * <p/>
	 * Read-only.
	 */
	Identity getIdentity(Transaction txn) throws DbException;

	Profile getProfile(Transaction transaction, String contextId)
			throws DbException;

	void setIdentityNetworkId(Transaction transaction, String networkId)
			throws DbException;

	void setIdentityProfilePicture(Transaction transaction,
			byte[] profilePicture)
			throws DbException;

	void addContact(Transaction transaction, Contact contact)
			throws DbException;

    void addEvent(Transaction transaction, HeliosEvent event)
            throws DbException;

    void addPendingContact(Transaction transaction, PendingContact p)
			throws DbException;

	boolean containsContact(Transaction transaction, ContactId contactId)
			throws DbException;

	boolean containsContext(Transaction transaction, String contextId)
			throws DbException;

	boolean containsEvent(Transaction transaction, String eventId)
            throws DbException;

	boolean containsPendingContact(Transaction transaction,
								   ContactId pendingContactId) throws DbException;

	boolean containsMessage(Transaction transaction, String messageId)
			throws DbException;

	boolean containsProfile(Transaction transaction, String contextId)
			throws DbException;

	Collection<Contact> getContacts(Transaction transaction)
			throws DbException;


	Collection<String> getContactIds(Transaction transaction, String contextId)
			throws DbException;

	Collection<Message> getFavourites(Transaction transaction,
			String contextId)
			throws DbException;

	PendingContact getPendingContact(Transaction transaction,
			ContactId pendingContactId) throws DbException;

	Contact getContact(Transaction transaction,
			ContactId contactId) throws DbException;

	HeliosEvent getEvent(Transaction transaction, String eventId)
			throws DbException;

	Collection<HeliosEvent> getEvents(Transaction transaction, String contextId)
            throws DbException;

	ContactId getContactIdByGroupId(Transaction transaction,
									String groupId)
			throws DbException;

	Group getContactGroup(Transaction transaction, ContactId contactId,
			String contextId)
			throws DbException;

	Group getGroup(Transaction transaction, String groupId)
			throws DbException;

	Collection<Group> getGroups(Transaction transaction, String contextId,
			GroupType groupType)
			throws DbException;

	Collection<Group> getForums(Transaction transaction,
			String contextId)
			throws DbException;

	Collection<Group> getGroups(Transaction transaction, GroupType groupType)
			throws DbException;

	Collection<Group> getForums(Transaction transaction)
			throws DbException;

	String getGroupContext(Transaction transaction, String groupId)
			throws DbException;

	Collection<ForumMember> getForumMembers(Transaction transaction,
			String groupId)
			throws DbException;

	Collection<PendingContact> getPendingContacts(
			Transaction transaction) throws DbException;

	Collection<ContextInvitation> getPendingContextInvitations(
			Transaction transaction) throws DbException;

	Collection<ContextInvitation> getPendingContextInvitations(
			Transaction transaction, String contextId) throws DbException;

	Collection<GroupInvitation> getGroupInvitations(Transaction transaction)
			throws DbException;

	Message getMessage(Transaction transaction, String messageId)
			throws DbException;

	String getMessageText(Transaction transaction, String messageId)
			throws DbException;

	Collection<String> getMessageIds(Transaction transaction,
			String groupId) throws DbException;

	Collection<MessageHeader> getMessageHeaders(Transaction transaction,
			String groupId) throws DbException;

	void removeContact(Transaction transaction, ContactId c)
			throws DbException;

	void removeEvent(Transaction transaction, String eventId)
            throws DbException;

	void addContext(Transaction transaction, DBContext c)
			throws DbException;

	void addToFavourites(Transaction transaction, String messageId)
			throws DbException;

	void removeFromFavourites(Transaction transaction, String messageId)
			throws DbException;

	void removeForumMember(Transaction transaction, String groupId,
			String fakeId)
			throws DbException;

	void removeForumMemberList(Transaction transaction, String groupId)
			throws DbException;

	void removeProfile(Transaction transaction, String contextId)
			throws DbException;

	MessageState getMessageState(Transaction transaction, String messageId)
			throws DbException;

	boolean setMessageState(Transaction transaction, String messageId,
			MessageState state)
			throws DbException;

	void addForumMember(Transaction transaction, ForumMember forumMember)
			throws DbException;

	void addForumMembers(Transaction transaction,
			Collection<ForumMember> forumMembers)
			throws DbException;

	void updateForumMemberRole(Transaction transaction,
			ForumMember forumMember)
			throws DbException;

	void updateForumMemberRole(Transaction transaction,
			String groupId, String fakeId, ForumMemberRole forumMemberRole,
			long timestamp)
			throws DbException;

	void updateProfile(Transaction transaction, Profile profile)
			throws DbException;

	void addMessage(Transaction transaction, Message message,
			MessageState state, String contextId, boolean incoming)
			throws DbException;

	Collection<DBContext> getContexts(Transaction transaction)
			throws DbException;

	DBContext getContext(Transaction transaction,
			String contextId)
			throws DbException;

	Integer getContextColor(Transaction transaction, String contextId)
			throws DbException;

	Metadata getContextMetadata(Transaction transaction,
			String contextId)
			throws DbException;

	Metadata getMessageMetadata(Transaction transaction,
			String messageId)
			throws DbException;

	Map<String, Metadata> getMessageMetadataByGroupId(
			Transaction transaction, String groupId)
			throws DbException;

	Metadata getGroupMetadata(Transaction transaction, String groupId)
			throws DbException;

	Map<String, Metadata> getGroupMetadata(
			Transaction transaction, String[] groupIds)
			throws DbException;

	void removeContext(Transaction transaction, String contextId)
			throws DbException;

	void removePendingContact(Transaction transaction,
			ContactId pendingContactId) throws DbException;

	void removePendingContext(Transaction transaction,
			String contextId) throws DbException;

	void removePendingGroup(Transaction transaction,
			String pendingGroupId) throws DbException;

	void removeContextInvitation(Transaction transaction,
			ContactId contactId, String pendingContextId) throws DbException;

	void removeGroupInvitation(Transaction transaction,
			ContactId contactId, String pendingGroupId) throws DbException;

	/**
	 * Returns all settings in the given namespace.
	 * <p/>
	 * Read-only.
	 */
	Settings getSettings(Transaction txn, String namespace) throws DbException;

	void mergeGroupMetadata(Transaction transaction, String groupId,
			Metadata meta)
			throws DbException;

	void mergeMessageMetadata(Transaction transaction, String messageId,
			Metadata meta)
			throws DbException;

	void mergeContextMetadata(Transaction transaction, String contextId,
			Metadata meta)
			throws DbException;

	/**
	 * Merges the given settings with the existing settings in the given
	 * namespace.
	 */
	void mergeSettings(Transaction txn, Settings s, String namespace)
			throws DbException;

	void addContextInvitation(Transaction transaction,
			ContextInvitation contextInvite)
			throws DbException;

	void addGroupInvitation(Transaction transaction,
			GroupInvitation groupInvite)
			throws DbException;

	void addGroup(Transaction transaction, Group group, byte[] descriptor,
			GroupType type)
			throws DbException;

	void addContactGroup(Transaction txn, Group group, ContactId contactId)
			throws DbException;
}
