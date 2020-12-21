package eu.h2020.helios_social.happ.helios.talk.db.database;

import eu.h2020.helios_social.happ.helios.talk.api.contact.event.ContactAddedEvent;
import eu.h2020.helios_social.happ.helios.talk.api.contact.event.ContactRemovedEvent;
import eu.h2020.helios_social.happ.helios.talk.api.contact.event.PendingContactAddedEvent;
import eu.h2020.helios_social.happ.helios.talk.api.contact.event.PendingContactRemovedEvent;
import eu.h2020.helios_social.happ.helios.talk.api.context.ContextInvitationAddedEvent;
import eu.h2020.helios_social.happ.helios.talk.api.context.ContextInvitationRemovedEvent;
import eu.h2020.helios_social.happ.helios.talk.api.context.RemovePendingContextEvent;
import eu.h2020.helios_social.happ.helios.talk.api.crypto.SecretKey;
import eu.h2020.helios_social.happ.helios.talk.api.db.CommitAction;
import eu.h2020.helios_social.happ.helios.talk.api.db.CommitAction.Visitor;
import eu.h2020.helios_social.happ.helios.talk.api.db.ContactExistsException;
import eu.h2020.helios_social.happ.helios.talk.api.db.ContextExistsException;
import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.happ.helios.talk.api.db.DbCallable;
import eu.h2020.helios_social.happ.helios.talk.api.db.GroupInvitationExistsException;
import eu.h2020.helios_social.happ.helios.talk.api.db.NoSuchGroupException;
import eu.h2020.helios_social.happ.helios.talk.api.db.NoSuchMessageException;
import eu.h2020.helios_social.happ.helios.talk.api.db.NoSuchPendingContextException;
import eu.h2020.helios_social.happ.helios.talk.api.db.NoSuchPendingGroupException;
import eu.h2020.helios_social.happ.helios.talk.api.db.PendingContextInvitationExistsException;
import eu.h2020.helios_social.happ.helios.talk.api.sync.event.GroupAddedEvent;
import eu.h2020.helios_social.happ.helios.talk.api.sync.event.GroupInvitationAddedEvent;
import eu.h2020.helios_social.happ.helios.talk.api.sync.event.GroupInvitationRemovedEvent;
import eu.h2020.helios_social.happ.helios.talk.api.sync.event.MessageAddedEvent;
import eu.h2020.helios_social.happ.helios.talk.api.sync.event.RemovePendingGroupEvent;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMember;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMemberRole;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageState;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.happ.helios.talk.api.db.DbRunnable;
import eu.h2020.helios_social.happ.helios.talk.api.db.EventAction;
import eu.h2020.helios_social.happ.helios.talk.api.db.Metadata;
import eu.h2020.helios_social.happ.helios.talk.api.db.MigrationListener;
import eu.h2020.helios_social.happ.helios.talk.api.db.NoSuchContactException;
import eu.h2020.helios_social.happ.helios.talk.api.db.NoSuchContextException;
import eu.h2020.helios_social.happ.helios.talk.api.db.NoSuchIdentityException;
import eu.h2020.helios_social.happ.helios.talk.api.db.NoSuchPendingContactException;
import eu.h2020.helios_social.happ.helios.talk.api.db.NullableDbCallable;
import eu.h2020.helios_social.happ.helios.talk.api.db.PendingContactExistsException;
import eu.h2020.helios_social.happ.helios.talk.api.db.TaskAction;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.happ.helios.talk.api.event.EventBus;
import eu.h2020.helios_social.happ.helios.talk.api.event.EventExecutor;
import eu.h2020.helios_social.happ.helios.talk.api.identity.Identity;
import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.ShutdownManager;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;
import eu.h2020.helios_social.happ.helios.talk.api.settings.Settings;
import eu.h2020.helios_social.happ.helios.talk.api.settings.event.SettingsUpdatedEvent;
import eu.h2020.helios_social.happ.helios.talk.api.sync.event.ContextAddedEvent;
import eu.h2020.helios_social.happ.helios.talk.api.sync.event.ContextRemovedEvent;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.Contact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContact;
import eu.h2020.helios_social.modules.groupcommunications.api.context.DBContext;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.Profile;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static eu.h2020.helios_social.happ.helios.talk.api.util.LogUtils.logDuration;
import static eu.h2020.helios_social.happ.helios.talk.api.util.LogUtils.logException;
import static eu.h2020.helios_social.happ.helios.talk.api.util.LogUtils.now;

@ThreadSafe
@NotNullByDefault
class DatabaseComponentImpl<T> implements DatabaseComponent {

	private static final Logger LOG =
			getLogger(DatabaseComponentImpl.class.getName());

	private final Database<T> db;
	private final Class<T> txnClass;
	private final EventBus eventBus;
	private final Executor eventExecutor;
	private final ShutdownManager shutdownManager;
	private final AtomicBoolean closed = new AtomicBoolean(false);
	private final ReentrantReadWriteLock lock =
			new ReentrantReadWriteLock(true);
	private final Visitor visitor = new CommitActionVisitor();

	@Inject
	DatabaseComponentImpl(Database<T> db, Class<T> txnClass, EventBus eventBus,
			@EventExecutor Executor eventExecutor,
			ShutdownManager shutdownManager) {
		this.db = db;
		this.txnClass = txnClass;
		this.eventBus = eventBus;
		this.eventExecutor = eventExecutor;
		this.shutdownManager = shutdownManager;
	}

	@Override
	public boolean open(SecretKey key, @Nullable MigrationListener listener)
			throws DbException {
		boolean reopened = db.open(key, listener);
		shutdownManager.addShutdownHook(() -> {
			try {
				close();
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
		return reopened;
	}

	@Override
	public void close() throws DbException {
		if (closed.getAndSet(true)) return;
		db.close();
	}

	@Override
	public Transaction startTransaction(boolean readOnly) throws DbException {
		// Don't allow reentrant locking
		if (lock.getReadHoldCount() > 0) throw new IllegalStateException();
		if (lock.getWriteHoldCount() > 0) throw new IllegalStateException();
		long start = now();
		if (readOnly) {
			lock.readLock().lock();
			logDuration(LOG, "Waiting for read lock", start);
		} else {
			lock.writeLock().lock();
			logDuration(LOG, "Waiting for write lock", start);
		}
		try {
			return new Transaction(db.startTransaction(), readOnly);
		} catch (DbException | RuntimeException e) {
			if (readOnly) lock.readLock().unlock();
			else lock.writeLock().unlock();
			throw e;
		}
	}

	@Override
	public void commitTransaction(Transaction transaction) throws DbException {
		T txn = txnClass.cast(transaction.unbox());
		if (transaction.isCommitted()) throw new IllegalStateException();
		transaction.setCommitted();
		db.commitTransaction(txn);
	}

	@Override
	public void endTransaction(Transaction transaction) {
		try {
			T txn = txnClass.cast(transaction.unbox());
			if (transaction.isCommitted()) {
				for (CommitAction a : transaction.getActions())
					a.accept(visitor);
			} else {
				db.abortTransaction(txn);
			}
		} finally {
			if (transaction.isReadOnly()) lock.readLock().unlock();
			else lock.writeLock().unlock();
		}
	}

	@Override
	public <E extends Exception> void transaction(boolean readOnly,
			DbRunnable<E> task) throws DbException, E {
		Transaction txn = startTransaction(readOnly);
		try {
			task.run(txn);
			commitTransaction(txn);
		} finally {
			endTransaction(txn);
		}
	}

	@Override
	public <R, E extends Exception> R transactionWithResult(boolean readOnly,
			DbCallable<R, E> task) throws DbException, E {
		Transaction txn = startTransaction(readOnly);
		try {
			R result = task.call(txn);
			commitTransaction(txn);
			return result;
		} finally {
			endTransaction(txn);
		}
	}

	@Override
	public <R, E extends Exception> R transactionWithNullableResult(
			boolean readOnly, NullableDbCallable<R, E> task)
			throws DbException, E {
		Transaction txn = startTransaction(readOnly);
		try {
			R result = task.call(txn);
			commitTransaction(txn);
			return result;
		} finally {
			endTransaction(txn);
		}
	}

	private T unbox(Transaction transaction) {
		if (transaction.isCommitted()) throw new IllegalStateException();
		return txnClass.cast(transaction.unbox());
	}

	@Override
	public void addIdentity(Transaction transaction, Identity i)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsIdentity(txn)) {
			db.addIdentity(txn, i);
		}
	}

	@Override
	public void addProfile(Transaction transaction, Profile profile)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContext(txn, profile.getContextId())) {
			throw new NoSuchContextException();
		}
		db.addProfile(txn, profile);
	}

	@Override
	public boolean containsIdentity(Transaction transaction)
			throws DbException {
		T txn = unbox(transaction);
		return db.containsIdentity(txn);
	}

	@Override
	public Identity getIdentity(Transaction transaction)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsIdentity(txn))
			throw new NoSuchIdentityException();
		return db.getIdentity(txn);
	}

	@Override
	public Profile getProfile(Transaction transaction, String contextId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContext(txn, contextId))
			throw new NoSuchContextException();
		return db.getProfile(txn, contextId);
	}

	@Override
	public void setIdentityNetworkId(Transaction transaction, String networkId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsIdentity(txn))
			throw new NoSuchIdentityException();
		db.setIdentityNetworkId(txn, networkId);
	}

	@Override
	public void setIdentityProfilePicture(Transaction transaction,
			byte[] profilePicture)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsIdentity(txn))
			throw new NoSuchIdentityException();
		db.setIdentityProfilePicture(txn, profilePicture);
	}

	@Override
	public void addContact(Transaction transaction, Contact contact)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		db.addContact(txn, contact);
		transaction.attach(new ContactAddedEvent(contact));
	}

	@Override
	public void addPendingContact(Transaction transaction, PendingContact p)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (db.containsContact(txn, p.getId()))
			throw new ContactExistsException(p.getId(), p.getAlias());
		if (db.containsPendingContact(txn, p.getId())) {
			PendingContact existing = db.getPendingContact(txn, p.getId());
			throw new PendingContactExistsException(existing);
		}
		db.addPendingContact(txn, p);
		transaction.attach(new PendingContactAddedEvent(p));
	}

	@Override
	public void addContextInvitation(Transaction transaction,
			ContextInvitation contextInvite)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (contextInvite.isIncoming() &&
				db.containsContext(txn, contextInvite.getContextId()))
			throw new ContextExistsException(contextInvite.getContextId());
		if (db.containsPendingContextInvitation(txn,
				contextInvite.getContactId(),
				contextInvite.getContextId()))
			throw new PendingContextInvitationExistsException(contextInvite);
		db.addContextInvitation(txn, contextInvite);
		transaction.attach(new ContextInvitationAddedEvent(contextInvite));
	}

	@Override
	public void addGroupInvitation(Transaction transaction,
			GroupInvitation groupInvitations)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (groupInvitations.isIncoming() &&
				db.containsGroup(txn, groupInvitations.getGroupId()))
			throw new ContextExistsException(groupInvitations.getGroupId());
		if (!db.containsContext(txn, groupInvitations.getContextId()))
			throw new NoSuchContextException();
		if (db.containsPendingGroupInvitation(txn,
				groupInvitations.getContactId(),
				groupInvitations.getGroupId()))
			throw new GroupInvitationExistsException(groupInvitations);
		db.addGroupInvitation(txn, groupInvitations);
		transaction.attach(new GroupInvitationAddedEvent(groupInvitations));
	}

	@Override
	public void addGroup(Transaction transaction, Group group,
			byte[] descriptor, GroupType type)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, group.getId())) {
			db.addGroup(txn, group, descriptor, type);
			transaction.attach(new GroupAddedEvent(group, type));
		}
	}

	@Override
	public void addContactGroup(Transaction transaction, Group group,
			ContactId contactId) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		db.addContactGroup(txn, group, contactId);
	}

	@Override
	public void addToFavourites(Transaction transaction, String messageId)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, messageId))
			throw new NoSuchMessageException();
		db.raiseFavouriteFlag(txn, messageId);
	}

	@Override
	public void removeFromFavourites(Transaction transaction, String messageId)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, messageId))
			throw new NoSuchMessageException();
		db.removeFavouriteFlag(txn, messageId);
	}

	@Override
	public void removeForumMember(Transaction transaction, String groupId,
			String fakeId)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, groupId))
			throw new NoSuchGroupException();
		db.removeForumMember(txn, groupId, fakeId);
	}

	@Override
	public void removeForumMemberList(Transaction transaction, String groupId)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, groupId))
			throw new NoSuchGroupException();
		db.removeForumMemberList(txn, groupId);
	}

	@Override
	public void removeProfile(Transaction transaction, String contextId)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContext(txn, contextId))
			throw new NoSuchContextException();
		db.removeProfile(txn, contextId);
	}

	@Override
	public MessageState getMessageState(Transaction transaction,
			String messageId) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, messageId))
			throw new NoSuchMessageException();
		return db.getMessageState(txn, messageId);
	}

	@Override
	public boolean setMessageState(Transaction transaction, String messageId,
			MessageState state) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, messageId))
			throw new NoSuchMessageException();
		return db.setMessageState(txn, messageId, state);
	}

	@Override
	public boolean containsContact(Transaction transaction, ContactId contactId)
			throws DbException {
		T txn = unbox(transaction);
		return db.containsContact(txn, contactId);
	}

	@Override
	public boolean containsContext(Transaction transaction, String contextId)
			throws DbException {
		T txn = unbox(transaction);
		return db.containsContext(txn, contextId);
	}

	@Override
	public boolean containsPendingContact(Transaction transaction,
			ContactId pendingContactId) throws DbException {
		T txn = unbox(transaction);
		return db.containsPendingContact(txn, pendingContactId);
	}

	@Override
	public boolean containsMessage(Transaction transaction, String messageId)
			throws DbException {
		T txn = unbox(transaction);
		return db.containsMessage(txn, messageId);
	}

	@Override
	public boolean containsProfile(Transaction transaction, String contextId)
			throws DbException {
		T txn = unbox(transaction);
		return db.containsProfile(txn, contextId);
	}

	@Override
	public Collection<Contact> getContacts(Transaction transaction)
			throws DbException {
		T txn = unbox(transaction);
		return db.getContacts(txn);
	}

	@Override
	public Collection<String> getContactIds(Transaction transaction,
			String contextId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContext(txn, contextId))
			throw new NoSuchContextException();
		return db.getContactIds(txn, contextId);
	}

	@Override
	public Collection<Message> getFavourites(Transaction transaction,
			String contextId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContext(txn, contextId))
			throw new NoSuchContextException();
		return db.getFavourites(txn, contextId);
	}

	@Override
	public PendingContact getPendingContact(Transaction transaction,
			ContactId pendingContactId) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsPendingContact(txn, pendingContactId))
			return null;
		return db.getPendingContact(txn, pendingContactId);
	}

	@Override
	public Contact getContact(Transaction transaction, ContactId contactId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContact(txn, contactId))
			return null;
		return db.getContact(txn, contactId);
	}

	@Override
	public ContactId getContactIdByGroupId(Transaction transaction,
			String groupId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, groupId))
			throw new NoSuchGroupException();
		return db.getContactIdByGroupId(txn, groupId);
	}

	@Override
	public Group getContactGroup(Transaction transaction, ContactId contactId,
			String contextId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContact(txn, contactId))
			throw new NoSuchContactException();
		if (!db.containsContext(txn, contextId))
			throw new NoSuchContextException();
		return db.getContactGroup(txn, contactId, contextId);
	}

	@Override
	public Group getGroup(Transaction transaction, String groupId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, groupId))
			throw new NoSuchGroupException();
		return db.getGroup(txn, groupId);
	}

	@Override
	public Collection<Group> getGroups(Transaction transaction,
			String contextId, GroupType groupType)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContext(txn, contextId))
			throw new NoSuchContextException();
		return db.getGroups(txn, contextId, groupType);
	}

	@Override
	public Collection<Group> getForums(Transaction transaction,
			String contextId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContext(txn, contextId))
			throw new NoSuchContextException();
		return db.getForums(txn, contextId);
	}

	@Override
	public Collection<Group> getGroups(Transaction transaction,
			GroupType groupType)
			throws DbException {
		T txn = unbox(transaction);
		return db.getGroups(txn, groupType);
	}

	@Override
	public Collection<Group> getForums(Transaction transaction)
			throws DbException {
		T txn = unbox(transaction);
		return db.getForums(txn);
	}

	@Override
	public String getGroupContext(Transaction transaction, String groupId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, groupId))
			throw new NoSuchContactException();
		return db.getGroupContext(txn, groupId);
	}

	@Override
	public Collection<ForumMember> getForumMembers(Transaction transaction,
			String groupId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, groupId))
			throw new NoSuchGroupException();
		return db.getForumMembers(txn, groupId);
	}

	@Override
	public Collection<PendingContact> getPendingContacts(
			Transaction transaction) throws DbException {
		T txn = unbox(transaction);
		return db.getPendingContacts(txn);
	}

	@Override
	public Collection<ContextInvitation> getPendingContextInvitations(
			Transaction transaction) throws DbException {
		T txn = unbox(transaction);
		return db.getPendingContextInvitations(txn);
	}

	@Override
	public Collection<ContextInvitation> getPendingContextInvitations(
			Transaction transaction, String contextId) throws DbException {
		T txn = unbox(transaction);
		return db.getPendingContextInvitations(txn, contextId);
	}

	@Override
	public Collection<GroupInvitation> getGroupInvitations(
			Transaction transaction)
			throws DbException {
		T txn = unbox(transaction);
		return db.getGroupInvitations(txn);
	}


	@Override
	public Message getMessage(Transaction transaction, String messageId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, messageId))
			throw new NoSuchMessageException();
		return db.getMessage(txn, messageId);
	}

	@Override
	public String getMessageText(Transaction transaction, String messageId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, messageId))
			throw new NoSuchMessageException();
		return db.getMessageText(txn, messageId);
	}

	@Override
	public Collection<String> getMessageIds(Transaction transaction,
			String groupId) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, groupId))
			throw new NoSuchGroupException();
		return db.getMessageIds(txn, groupId);
	}

	@Override
	public Collection<MessageHeader> getMessageHeaders(Transaction transaction,
			String groupId) throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, groupId))
			throw new NoSuchGroupException();
		return db.getMessageHeaders(txn, groupId);
	}

	@Override
	public void removeContact(Transaction transaction, ContactId c)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContact(txn, c))
			throw new NoSuchContactException();
		db.removeContact(txn, c);
		transaction.attach(new ContactRemovedEvent(c));
	}

	@Override
	public void addContext(Transaction transaction, DBContext context)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContext(txn, context.getId())) {
			db.addContext(txn, context);
			transaction.attach(new ContextAddedEvent(context));
		}
	}

	@Override
	public void addForumMember(Transaction transaction, ForumMember forumMember)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, forumMember.getGroupId())) {
			throw new NoSuchGroupException();
		}
		db.addForumMember(txn, forumMember);
	}

	@Override
	public void addForumMembers(Transaction transaction,
			Collection<ForumMember> forumMembers)
			throws DbException {
		T txn = unbox(transaction);
		db.addForumMembers(txn, forumMembers);
	}

	@Override
	public void updateForumMemberRole(Transaction transaction,
			ForumMember forumMember)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, forumMember.getGroupId())) {
			throw new NoSuchGroupException();
		}
		db.updateForumMemberRole(txn, forumMember);
	}

	@Override
	public void updateForumMemberRole(Transaction transaction, String groupId,
			String fakeId, ForumMemberRole forumMemberRole, long timestamp)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, groupId)) {
			throw new NoSuchGroupException();
		}
		db.updateForumMemberRole(txn, groupId, fakeId, forumMemberRole,
				timestamp);
	}

	@Override
	public void updateProfile(Transaction transaction, Profile profile)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContext(txn, profile.getContextId())) {
			throw new NoSuchContextException();
		}
		db.updateProfile(txn, profile);
	}

	@Override
	public void addMessage(Transaction transaction, Message message,
			MessageState state, String contextId, boolean incoming)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, message.getId())) {
			db.addMessage(txn, message, state, contextId, incoming);
			transaction.attach(new MessageAddedEvent(message, incoming, state));
		}
	}

	@Override
	public Collection<DBContext> getContexts(Transaction transaction)
			throws DbException {
		T txn = unbox(transaction);
		return db.getContexts(txn);
	}

	@Override
	public DBContext getContext(Transaction transaction,
			String contextId)
			throws DbException {
		T txn = unbox(transaction);
		return db.getContext(txn, contextId);
	}

	@Override
	public Integer getContextColor(Transaction transaction, String contextId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContext(txn, contextId))
			throw new NoSuchContextException();
		return db.getContextColor(txn, contextId);
	}

	@Override
	public Metadata getContextMetadata(Transaction transaction,
			String contextId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsContext(txn, contextId))
			throw new NoSuchContextException();
		return db.getContextMetadata(txn, contextId);
	}

	@Override
	public Metadata getMessageMetadata(Transaction transaction,
			String messageId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, messageId))
			throw new NoSuchMessageException();
		return db.getMessageMetadata(txn, messageId);
	}

	@Override
	public Map<String, Metadata> getMessageMetadataByGroupId(
			Transaction transaction, String groupId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, groupId))
			throw new NoSuchGroupException();
		return db.getMessageMetadataByGroupId(txn, groupId);
	}

	@Override
	public Metadata getGroupMetadata(Transaction transaction, String groupId)
			throws DbException {
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, groupId))
			throw new NoSuchGroupException();
		return db.getGroupMetadata(txn, groupId);
	}

	@Override
	public Map<String, Metadata> getGroupMetadata(
			Transaction transaction, String[] groupIds)
			throws DbException {
		T txn = unbox(transaction);
		return db.getGroupMetadata(txn, groupIds);
	}

	@Override
	public void removeContext(Transaction transaction, String contextId)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContext(txn, contextId))
			throw new NoSuchContextException();
		db.removeContext(txn, contextId);
		transaction.attach(new ContextRemovedEvent(contextId));
	}

	@Override
	public void removePendingContact(Transaction transaction,
			ContactId pendingContactId) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsPendingContact(txn, pendingContactId))
			throw new NoSuchPendingContactException();
		db.removePendingContact(txn, pendingContactId);
		transaction.attach(new PendingContactRemovedEvent(pendingContactId));
	}

	@Override
	public void removePendingContext(Transaction transaction,
			String pendingContextId) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsPendingContext(txn, pendingContextId))
			throw new NoSuchPendingContextException();
		db.removePendingContext(txn, pendingContextId);
		transaction.attach(new RemovePendingContextEvent(pendingContextId));
	}

	@Override
	public void removePendingGroup(Transaction transaction,
			String pendingGroupId) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsPendingGroup(txn, pendingGroupId))
			throw new NoSuchPendingGroupException();
		db.removePendingGroup(txn, pendingGroupId);
		transaction.attach(new RemovePendingGroupEvent(pendingGroupId));
	}

	@Override
	public void removeContextInvitation(Transaction transaction,
			ContactId contactId, String pendingContextId) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsPendingContextInvitation(txn, contactId,
				pendingContextId))
			throw new NoSuchPendingContextException();
		db.removeContextInvitation(txn, contactId, pendingContextId);
		transaction.attach(new ContextInvitationRemovedEvent(contactId,
				pendingContextId));
	}

	@Override
	public void removeGroupInvitation(Transaction transaction,
			ContactId contactId, String pendingGroupId) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsPendingGroup(txn, pendingGroupId))
			throw new NoSuchPendingGroupException();
		db.removeGroupInvitation(txn, contactId, pendingGroupId);
		transaction.attach(new GroupInvitationRemovedEvent(contactId,
				pendingGroupId));
	}

	@Override
	public Settings getSettings(Transaction transaction, String namespace)
			throws DbException {
		T txn = unbox(transaction);
		return db.getSettings(txn, namespace);
	}

	@Override
	public void mergeGroupMetadata(Transaction transaction, String groupId,
			Metadata meta)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsGroup(txn, groupId))
			throw new NoSuchGroupException();
		db.mergeGroupMetadata(txn, groupId, meta);
	}

	@Override
	public void mergeMessageMetadata(Transaction transaction, String messageId,
			Metadata meta)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsMessage(txn, messageId))
			throw new NoSuchMessageException();
		db.mergeMessageMetadata(txn, messageId, meta);
	}

	@Override
	public void mergeContextMetadata(Transaction transaction, String contextId,
			Metadata meta)
			throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		if (!db.containsContext(txn, contextId))
			throw new NoSuchContextException();
		db.mergeContextMetadata(txn, contextId, meta);
	}

	@Override
	public void mergeSettings(Transaction transaction, Settings s,
			String namespace) throws DbException {
		if (transaction.isReadOnly()) throw new IllegalArgumentException();
		T txn = unbox(transaction);
		Settings old = db.getSettings(txn, namespace);
		Settings merged = new Settings();
		merged.putAll(old);
		merged.putAll(s);
		if (!merged.equals(old)) {
			db.mergeSettings(txn, s, namespace);
			transaction.attach(new SettingsUpdatedEvent(namespace, merged));
		}
	}

	private class CommitActionVisitor implements Visitor {

		@Override
		public void visit(EventAction a) {
			eventBus.broadcast(a.getEvent());
		}

		@Override
		public void visit(TaskAction a) {
			eventExecutor.execute(a.getTask());
		}
	}
}
