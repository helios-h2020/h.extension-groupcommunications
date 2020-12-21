package eu.h2020.helios_social.happ.helios.talk.db.database;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

import eu.h2020.helios_social.happ.helios.talk.api.crypto.SecretKey;
import eu.h2020.helios_social.happ.helios.talk.api.db.DataTooNewException;
import eu.h2020.helios_social.happ.helios.talk.api.db.DataTooOldException;
import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMember;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMemberRole;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageState;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.happ.helios.talk.api.db.Metadata;
import eu.h2020.helios_social.happ.helios.talk.api.db.MigrationListener;
import eu.h2020.helios_social.happ.helios.talk.api.identity.Identity;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;
import eu.h2020.helios_social.happ.helios.talk.api.settings.Settings;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.Contact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContact;
import eu.h2020.helios_social.modules.groupcommunications.api.context.DBContext;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.Profile;

import javax.annotation.Nullable;

/**
 * A low-level interface to the database ({@link DatabaseComponent} provides a
 * high-level interface).
 * <p/>
 * Most operations take a transaction argument, which is obtained by calling
 * {@link #startTransaction()}. Every transaction must be terminated by calling
 * either {@link #abortTransaction(Object) abortTransaction(T)} or
 * {@link #commitTransaction(Object) commitTransaction(T)}, even if an
 * exception is thrown.
 */
@NotNullByDefault
interface Database<T> {

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
	 * Prevents new transactions from starting, waits for all current
	 * transactions to finish, and closes the database.
	 */
	void close() throws DbException;

	/**
	 * Starts a new transaction and returns an object representing it.
	 */
	T startTransaction() throws DbException;

	/**
	 * Aborts the given transaction - no changes made during the transaction
	 * will be applied to the database.
	 */
	void abortTransaction(T txn);

	/**
	 * Commits the given transaction - all changes made during the transaction
	 * will be applied to the database.
	 */
	void commitTransaction(T txn) throws DbException;

	boolean containsIdentity(T txn)
			throws DbException;

	Identity getIdentity(T txn)
			throws DbException;

	Profile getProfile(T txn, String contextId)
			throws DbException;

	void setIdentityNetworkId(T txn, String networkId)
			throws DbException;

	void setIdentityProfilePicture(T txn, byte[] profilePic)
			throws DbException;

	void removeProfile(T txn, String contextId) throws
			DbException;

	void addIdentity(T txn, Identity identity)
			throws DbException;

	void addContact(T txn, Contact contact)
			throws DbException;

	void addGroup(T txn, Group group, byte[] descriptor, GroupType groupType)
			throws DbException;

	void addForumMember(T txn, ForumMember forumMember)
			throws DbException;

	void addForumMembers(T txn,
			Collection<ForumMember> forumMembers)
			throws DbException;

	void  updateForumMemberRole(T txn, ForumMember forumMember)
			throws DbException;

	void updateForumMemberRole(T txn, String groupId,
			String fakeId, ForumMemberRole forumMemberRole, long timestamp)
			throws DbException;

	void updateProfile(T txn, Profile p) throws DbException;

	void addContactGroup(T txn, Group group,
			ContactId contactId)
			throws DbException;

	void raiseFavouriteFlag(T txn, String messageId)
			throws DbException;

	void removeFavouriteFlag(T txn, String messageId)
			throws DbException;

	void addMessage(T txn, Message message, MessageState state,
			String contextId, boolean incoming) throws DbException;

	void addContext(T txn, DBContext c)
			throws DbException;

	void addProfile(T txn, Profile p) throws DbException;

	boolean setMessageState(T txn, String messageId,
			MessageState state)
			throws DbException;

	boolean containsContact(T txn, ContactId contactId)
			throws DbException;

	boolean containsContext(T txn, String contextId)
			throws DbException;

	boolean containsGroup(T txn, String groupId)
			throws DbException;

	Contact getContact(T txn, ContactId cid)
			throws DbException;

	boolean containsContactGroup(T txn, ContactId contactId,
			String contextId)
			throws DbException;

	Collection<Contact> getContacts(T txn) throws DbException;

	Collection<String> getContactIds(T txn, String contextId)
			throws DbException;

	Integer getContextColor(T txn, String contextId)
			throws DbException;

	Collection<DBContext> getContexts(T txn)
			throws DbException;

	DBContext getContext(T txn, String contextId)
			throws DbException;

	Group getContactGroup(T txn, ContactId contactId,
			String contextId)
			throws DbException;

	Group getGroup(T txn, String groupId)
			throws DbException;

	Collection<Group> getGroups(T txn, String contextId,
			GroupType groupType)
			throws DbException;

	Collection<Group> getForums(T txn, String contextId)
			throws DbException;

	Collection<ForumMember> getForumMembers(T txn,
			String groupId) throws DbException;

	Collection<Group> getGroups(T txn, GroupType groupType)
			throws DbException;

	Collection<Group> getForums(T txn)
			throws DbException;

	String getGroupContext(T txn, String groupId)
			throws DbException;

	String getContextId(T txn, String groupId)
			throws DbException;

	Metadata getContextMetadata(T txn, String contextId)
			throws DbException;

	Metadata getMessageMetadata(T txn, String messageId)
			throws DbException;

	Map<String, Metadata> getMessageMetadataByGroupId(T txn,
			String groupId) throws DbException;

	Metadata getGroupMetadata(T txn, String groupId)
			throws DbException;

	Map<String, Metadata> getGroupMetadata(T txn,
			String[] contextIds) throws DbException;

	void removeContact(T txn, ContactId c)
			throws DbException;

	void removeContext(T txn, String contextId) throws DbException;

	void removeForumMember(T txn, String groupId, String fakeId)
			throws DbException;

	void removeForumMemberList(T txn, String groupId)
			throws DbException;

	void removeContextMetadata(T txn, String contextId)
			throws DbException;

	void setContactAlias(T txn, ContactId c,
			@Nullable String alias) throws DbException;

	void addPendingContact(T txn, PendingContact p)
			throws DbException;

	void addContextInvitation(T txn, ContextInvitation contextInvite)
			throws DbException;

	void addGroupInvitation(T txn, GroupInvitation groupInvite)
			throws DbException;

	boolean containsPendingContact(T txn,
			ContactId pendingContactId)
			throws DbException;

	boolean containsPendingContext(T txn,
			String pendingContextId)
			throws DbException;

	boolean containsPendingGroup(T txn,
			String pendingGroupId)
			throws DbException;

	boolean containsPendingContextInvitation(T txn, ContactId contactId,
			String pendingContextId)
			throws DbException;

	boolean containsPendingGroupInvitation(T txn,
			ContactId contactId,
			String pendingGroupId)
			throws DbException;

	boolean containsProfile(T txn, String contextId)
			throws DbException;

	boolean containsMessage(T txn,
			String messageId)
			throws DbException;

	PendingContact getPendingContact(T txn,
			ContactId pendingContactId)
			throws DbException;

	Collection<PendingContact> getPendingContacts(T txn)
			throws DbException;

	Collection<ContextInvitation> getPendingContextInvitations(T txn)
			throws DbException;

	Collection<GroupInvitation> getGroupInvitations(T txn)
			throws DbException;

	Collection<ContextInvitation> getPendingContextInvitations(T txn,
			String contextId)
			throws DbException;

	Message getMessage(T txn, String messageId)
			throws DbException;

	MessageState getMessageState(T txn, String messageId)
			throws DbException;

	Collection<String> getMessageIds(T txn, String groupId)
			throws DbException;

	Collection<MessageHeader> getMessageHeaders(T txn,
			String groupId)
			throws DbException;

	Collection<Message> getFavourites(T txn,
			String contextId)
			throws DbException;

	String getMessageText(T txn, String messageId)
			throws DbException;

	void removePendingContact(T txn, ContactId pendingContactId)
			throws DbException;

	void removePendingContext(T txn, String contextId)
			throws DbException;

	void removePendingGroup(T txn, String groupId)
			throws DbException;

	void removeContextInvitation(T txn, ContactId contactId,
			String contextId)
			throws DbException;

	void removeGroupInvitation(T txn, ContactId contactId,
			String pendingGroupId)
			throws DbException;

	Settings getSettings(T txn, String namespace)
			throws DbException;

	void mergeContextMetadata(T txn, String contextId,
			Metadata meta)
			throws DbException;

	ContactId getContactIdByGroupId(T txn, String groupId)
			throws DbException;

	void mergeMessageMetadata(T txn, String messageId,
			Metadata meta) throws DbException;

	void mergeGroupMetadata(T txn, String groupId,
			Metadata meta)
			throws DbException;

	void mergeSettings(T txn, Settings s, String namespace)
			throws DbException;
}
