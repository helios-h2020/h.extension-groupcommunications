package eu.h2020.helios_social.modules.groupcommunications.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.core.context.Context;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfDictionary;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfEntry;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfList;
import eu.h2020.helios_social.happ.helios.talk.api.data.Encoder;
import eu.h2020.helios_social.happ.helios.talk.api.data.Parser;
import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.happ.helios.talk.api.db.Metadata;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.context.ContextType;
import eu.h2020.helios_social.modules.groupcommunications.api.context.DBContext;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.GeneralContextProxy;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.LocationContextProxy;

import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_FORUMS;
import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_KEY_MEMBERS;
import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_LAT;
import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_LNG;
import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_PRIVATE_GROUPS;
import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_RADIUS;
import static java.util.logging.Logger.getLogger;


public class ContextManagerImpl implements ContextManager<Transaction> {
	private static final Logger LOG =
			getLogger(ContextManagerImpl.class.getName());

	private final DatabaseComponent db;
	private final Encoder encoder;
	private final Parser parser;

	@Inject
	ContextManagerImpl(DatabaseComponent db, Encoder encoder,
			Parser parser) {
		this.db = db;
		this.encoder = encoder;
		this.parser = parser;
	}

	@Override
	public void addContext(Context context) throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			if (context instanceof LocationContextProxy) {
				addContext(txn, (LocationContextProxy) context);
			} else if (context instanceof GeneralContextProxy) {
				addContext(txn, (GeneralContextProxy) context);
			}
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void addPendingContext(ContextInvitation contextInvitation)
			throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.addContextInvitation(txn, contextInvitation);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public Collection<ContextInvitation> getPendingContextInvitations()
			throws DbException {
		Transaction txn = db.startTransaction(false);
		Collection<ContextInvitation> contextInvitations;
		try {
			contextInvitations = db.getPendingContextInvitations(txn);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return contextInvitations;
	}

	@Override
	public Collection<ContextInvitation> getPendingContextInvitations(Transaction txn)
			throws DbException {
		return db.getPendingContextInvitations(txn);
	}

	@Override
	public void removePendingContext(String pendingContextId)
			throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.removePendingContext(txn, pendingContextId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void removeContextInvitation(ContactId contactId,
			String pendingContextId)
			throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.removeContextInvitation(txn, contactId, pendingContextId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public boolean contextExists(String contextId) throws DbException {
		Transaction txn = db.startTransaction(true);
		boolean exists;
		try {
			exists = db.containsContext(txn, contextId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return exists;
	}

	@Override
	public void addContext(Transaction txn, GeneralContextProxy context)
			throws DbException {
		try {
			DBContext dbContext =
					new DBContext(context.getId(), context.getName(),
							context.getColor(), ContextType.GENERAL);
			db.addContext(txn, dbContext);
			BdfDictionary meta = BdfDictionary.of(
					new BdfEntry(CONTEXT_KEY_MEMBERS, new BdfList()),
					new BdfEntry(CONTEXT_PRIVATE_GROUPS, new BdfList()),
					new BdfEntry(CONTEXT_FORUMS, new BdfList())
			);
			db.mergeContextMetadata(txn, context.getId(), encoder.encodeMetadata(meta));
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void addContext(Transaction txn, LocationContextProxy context)
			throws DbException {
		try {
			DBContext dbContext =
					new DBContext(context.getId(), context.getName(),
							context.getColor(), ContextType.GENERAL);
			db.addContext(txn, dbContext);
			BdfDictionary meta = BdfDictionary.of(
					new BdfEntry(CONTEXT_LAT, context.getLat()),
					new BdfEntry(CONTEXT_LNG, context.getLon()),
					new BdfEntry(CONTEXT_RADIUS, context.getRadius()),
					new BdfEntry(CONTEXT_KEY_MEMBERS, new BdfList()),
					new BdfEntry(CONTEXT_PRIVATE_GROUPS, new BdfList()),
					new BdfEntry(CONTEXT_FORUMS, new BdfList())
			);
			db.mergeContextMetadata(txn, context.getId(), encoder.encodeMetadata(meta));
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void removeContext(String contextId) throws DbException {
		db.transaction(false, txn -> {
			db.removeContext(txn, contextId);
		});
	}

	@Override
	public Collection<DBContext> getContexts() throws DbException {
		Transaction txn = db.startTransaction(true);
		Collection<DBContext> contexts = new ArrayList<>();
		try {
			contexts = db.getContexts(txn);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return contexts;
	}

	@Override
	public Context getContext(String contextId)
			throws DbException, FormatException {
		Transaction txn = db.startTransaction(true);
		Context context = null;
		try {
			context = getContext(txn, contextId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return context;
	}

	@Override
	public Integer getContextColor(String contextId) throws DbException {
		return db.transactionWithResult(true, txn ->
				db.getContextColor(txn, contextId));
	}

	@Override
	public boolean isMember(Transaction txn, String contextId, ContactId cid)
			throws DbException, FormatException {
		for (ContactId member : getMembers(txn, contextId)) {
			if (member.equals(cid)) return true;
		}
		return false;
	}

	@Override
	public boolean isMember(String contextId, ContactId cid)
			throws DbException, FormatException {
		Transaction txn = db.startTransaction(true);
		try {
			boolean canBeShared = this.isMember(txn, contextId, cid);
			db.commitTransaction(txn);
			return canBeShared;
		} finally {
			db.endTransaction(txn);
		}

	}

	@Override
	public Collection<ContextInvitation> getOutgoingContextInvitations(String contextId)
			throws DbException {
		Transaction txn = db.startTransaction(true);
		Collection<ContextInvitation> contextInvitation;
		try {
			contextInvitation =
					db.getPendingContextInvitations(txn, contextId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return contextInvitation;
	}

	@Override
	public boolean belongsToContext(ContactId contactId, String contextId)
			throws DbException {
		Transaction txn = db.startTransaction(true);
		Group group;
		try {
			group = db.getContactGroup(txn, contactId, contextId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return group != null;
	}

	@Override
	public ContextCount getStats(String contextId) throws DbException,
			FormatException {
		Transaction txn = db.startTransaction(true);
		ContextCount stats;
		try {
			stats = getStats(txn, contextId);
			db.commitTransaction(txn);

		} finally {
			db.endTransaction(txn);
		}
		return stats;
	}

	@Override
	public void addMember(String contextId, ContactId cid)
			throws DbException, FormatException {
		Transaction txn = db.startTransaction(false);
		try {
			addMember(txn, contextId, cid);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void addMember(Transaction txn, String contextId, ContactId a)
			throws DbException, FormatException {
		Metadata metadata = db.getContextMetadata(txn, contextId);
		BdfDictionary meta = parser.parseMetadata(metadata);
		BdfList members = meta.getList(CONTEXT_KEY_MEMBERS);
		members.add(a.getId().getBytes());
		db.mergeContextMetadata(txn, contextId, encoder.encodeMetadata(meta));
	}

	@Override
	public Collection<ContactId> getMembers(String contextId)
			throws DbException, FormatException {
		Transaction txn = db.startTransaction(true);
		Collection<ContactId> members;
		try {
			members = getMembers(txn, contextId);
			db.commitTransaction(txn);

		} finally {
			db.endTransaction(txn);
		}
		return members;
	}

	@Override
	public Collection<String> getPrivateGroups(String contextId)
			throws DbException, FormatException {
		Collection<String> groupIds;
		Transaction txn = db.startTransaction(true);
		try {
			groupIds = getPrivateGroups(txn, contextId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return groupIds;
	}

	@Override
	public Collection<String> getForums(String contextId)
			throws DbException, FormatException {
		Collection<String> forumIds;
		Transaction txn = db.startTransaction(true);
		try {
			forumIds = getForums(txn, contextId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return forumIds;
	}

	@Override
	public void addForum(Transaction txn, String contextId, String forumId)
			throws DbException, FormatException {
		Metadata metadata = db.getContextMetadata(txn, contextId);
		BdfDictionary meta = parser.parseMetadata(metadata);
		BdfList groups = meta.getList(CONTEXT_FORUMS);
		groups.add(forumId.getBytes());
		db.mergeContextMetadata(txn, contextId, encoder.encodeMetadata(meta));
	}

	@Override
	public void addForum(String contextId, String forumId)
			throws DbException, FormatException {
		Transaction txn = db.startTransaction(false);
		try {
			addForum(txn, contextId, forumId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void addPrivateGroup(String contextId, String groupId)
			throws DbException, FormatException {
		Transaction txn = db.startTransaction(false);
		try {
			addPrivateGroup(txn, contextId, groupId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void addPrivateGroup(Transaction txn, String contextId,
			String groupId)
			throws DbException, FormatException {
		Metadata metadata = db.getContextMetadata(txn, contextId);
		BdfDictionary meta = parser.parseMetadata(metadata);
		BdfList groups = meta.getList(CONTEXT_PRIVATE_GROUPS);
		groups.add(groupId.getBytes());
		db.mergeContextMetadata(txn, contextId, encoder.encodeMetadata(meta));
	}

	@Override
	public Collection<ContactId> getMembers(Transaction txn, String contextId)
			throws DbException, FormatException {
		Metadata metadata = db.getContextMetadata(txn, contextId);
		BdfDictionary meta = parser.parseMetadata(metadata);
		BdfList list = meta.getList(CONTEXT_KEY_MEMBERS);
		HashSet<ContactId> members = new HashSet<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			String id = list.getString(i);
			members.add(new ContactId(id));
		}
		return members;

	}

	private Collection<String> getPrivateGroups(Transaction txn,
			String contextId)
			throws DbException, FormatException {
		Metadata metadata = db.getContextMetadata(txn, contextId);
		BdfDictionary meta = parser.parseMetadata(metadata);
		BdfList list = meta.getList(CONTEXT_PRIVATE_GROUPS);
		HashSet<String> groups = new HashSet<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			String id = list.getString(i);
			groups.add(id);
		}
		return groups;

	}

	private Collection<String> getForums(Transaction txn, String contextId)
			throws DbException, FormatException {
		Metadata metadata = db.getContextMetadata(txn, contextId);
		BdfDictionary meta = parser.parseMetadata(metadata);
		BdfList list = meta.getList(CONTEXT_FORUMS);
		HashSet<String> forums = new HashSet<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			String id = list.getString(i);
			forums.add(id);
		}
		return forums;
	}

	private ContextCount getStats(Transaction txn, String contextId)
			throws DbException, FormatException {
		Metadata metadata = db.getContextMetadata(txn, contextId);
		BdfDictionary meta = parser.parseMetadata(metadata);
		BdfList members = meta.getList(CONTEXT_KEY_MEMBERS);
		BdfList groups = meta.getList(CONTEXT_PRIVATE_GROUPS);
		BdfList forums = meta.getList(CONTEXT_FORUMS);

		return new ContextCount(
				members.size(),
				groups.size(),
				forums.size()
		);
	}

	private Context getContext(Transaction txn, String contextId)
			throws DbException, FormatException {
		DBContext dbContext = db.getContext(txn, contextId);

		if (dbContext.getContextType().equals(ContextType.GENERAL)) {
			return new GeneralContextProxy(dbContext.getId(),
					dbContext.getName(), dbContext.getColor(), false);
		} else if (dbContext.getContextType()
				.equals(ContextType.LOCATION)) {
			Metadata metadata = db.getContextMetadata(txn, contextId);
			BdfDictionary meta = parser.parseMetadata(metadata);
			Double lat = meta.getDouble(CONTEXT_LAT);
			Double lng = meta.getDouble(CONTEXT_LNG);
			Double radius = meta.getDouble(CONTEXT_RADIUS);
			return new LocationContextProxy(dbContext.getId(),
					dbContext.getName(), dbContext.getColor(), lat, lng,
					radius);
		} else {
			//TODO: Temporal & Spatiotemporal Contexts
			return null;
		}
	}
}
