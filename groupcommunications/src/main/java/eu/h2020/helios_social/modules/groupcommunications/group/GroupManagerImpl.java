package eu.h2020.helios_social.modules.groupcommunications.group;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import eu.h2020.helios_social.happ.helios.talk.api.data.BdfDictionary;
import eu.h2020.helios_social.happ.helios.talk.api.data.Parser;
import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.happ.helios.talk.api.db.Metadata;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.happ.helios.talk.api.identity.IdentityManager;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumType;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.LocationForum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.SeasonalForum;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumManager;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupManager;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroupManager;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.utils.Pair;

import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.GROUP_SHOW_TRUE_SELF;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FAKE_ID;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FUNNY_NAME;

public class GroupManagerImpl implements GroupManager<Transaction> {

	private final PrivateGroupManager privateGroupManager;
	private final ForumManager forumManager;
	private final DatabaseComponent db;
	private final IdentityManager identityManager;
	private final Parser parser;

	@Inject
	public GroupManagerImpl(DatabaseComponent db,
			PrivateGroupManager privateGroupManager, ForumManager forumManager,
			IdentityManager identityManager, Parser parse) {
		this.db = db;
		this.privateGroupManager = privateGroupManager;
		this.identityManager = identityManager;
		this.parser = parse;
		this.forumManager = forumManager;
	}

	@Override
	public void addGroupInvitation(GroupInvitation groupInvitation)
			throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.addGroupInvitation(txn, groupInvitation);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void addGroup(Group group)
			throws FormatException, DbException {
		Transaction txn = db.startTransaction(false);
		try {
			if (group instanceof PrivateGroup) {
				privateGroupManager.addPrivateGroup(txn, (PrivateGroup) group);
			} else if (group instanceof Forum) {
				//TODO add forum on db
			} else {
				//TODO contact Group
			}
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void addGroup(Transaction txn, Group group)
			throws FormatException, DbException {
		if (group instanceof PrivateGroup) {
			privateGroupManager.addPrivateGroup(txn, (PrivateGroup) group);
		} else {
			Forum forum = (Forum) group;
			if (forum instanceof LocationForum) {
				forumManager.addForum(txn, (LocationForum) forum,
						ForumType.LOCATION, false);
			} else if (forum instanceof SeasonalForum) {
				forumManager.addForum(txn, (SeasonalForum) forum,
						ForumType.SEASONAL, false);
			} else {
				forumManager.addForum(txn, forum, ForumType.GENERAL, false);
			}
		}
	}

	@Override
	public Group getGroup(String groupId, GroupType groupType)
			throws FormatException, DbException {
		if (groupType.equals(GroupType.PrivateGroup)) {
			return privateGroupManager.getPrivateGroup(groupId);
		} else if (groupType.getValue() > 1) {
			return forumManager.getForum(groupId);
		} else {
			//TODO contact Group
			return null;
		}
	}

	@Override
	public Collection<Group> getGroups(String contextId)
			throws DbException, FormatException {
		Collection<Group> groups = new ArrayList();
		groups.addAll(privateGroupManager.getPrivateGroups(contextId));
		groups.addAll(forumManager.getForums(contextId));
		return groups;
	}

	@Override
	public Collection<Group> getGroups(Transaction txn)
			throws DbException, FormatException {
		Collection<Group> groups = new ArrayList();
		groups.addAll(privateGroupManager.getPrivateGroups(txn));
		groups.addAll(forumManager.getForums(txn));
		return groups;
	}

	@Override
	public Pair<String, String> getFakeIdentity(String groupId)
			throws DbException, FormatException {
		Transaction txn = db.startTransaction(true);
		Pair<String, String> fakeIdentity;
		try {
			fakeIdentity = getFakeIdentity(txn, groupId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return fakeIdentity;
	}

	@Override
	public Pair<String, String> getFakeIdentity(Transaction txn, String groupId)
			throws DbException, FormatException {
		Pair<String, String> fakeIdentity;

		Metadata metadata = db.getGroupMetadata(txn, groupId);
		BdfDictionary meta = parser.parseMetadata(metadata);
		String funnyName = null;
		if (!meta.getBoolean(GROUP_SHOW_TRUE_SELF)) {
			funnyName = meta.getString(PEER_FUNNY_NAME);
		}
		String fakeId = meta.getString(PEER_FAKE_ID);
		fakeIdentity = new Pair(funnyName, fakeId);
		return fakeIdentity;

	}

	@Override
	public void addGroupInvitation(Transaction txn,
			GroupInvitation groupInvitation)
			throws DbException {
		db.addGroupInvitation(txn, groupInvitation);
	}

	@Override
	public void removeGroupInvitation(ContactId contactId, String groupId)
			throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.removeGroupInvitation(txn, contactId,
					groupId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void removeGroupInvitation(Transaction txn,
			ContactId contactId, String groupId)
			throws DbException {
		db.removeGroupInvitation(txn, contactId, groupId);
	}

	@Override
	public Collection<GroupInvitation> getGroupInvitations()
			throws DbException {
		Transaction txn = db.startTransaction(true);
		Collection<GroupInvitation> groupInvitations;
		try {
			groupInvitations = db.getGroupInvitations(txn);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return groupInvitations;
	}

	@Override
	public boolean isInvitationAllowed(String groupId, GroupType groupType)
			throws FormatException, DbException {
		if (groupType == GroupType.PrivateGroup) {
			PrivateGroup privateGroup =
					privateGroupManager.getPrivateGroup(groupId);
			return privateGroup.getOwner()
					.equals(identityManager.getIdentity().getNetworkId());
		} else {
			//forums are sharable entities and can be shared by all forum members
			return true;
		}
	}

}
