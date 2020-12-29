package eu.h2020.helios_social.modules.groupcommunications.forum;

import com.github.javafaker.Faker;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import eu.h2020.helios_social.happ.helios.talk.api.data.BdfDictionary;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfEntry;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfList;
import eu.h2020.helios_social.happ.helios.talk.api.data.Encoder;
import eu.h2020.helios_social.happ.helios.talk.api.data.Parser;
import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.happ.helios.talk.api.db.Metadata;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.happ.helios.talk.api.identity.IdentityManager;
import eu.h2020.helios_social.happ.helios.talk.api.system.Clock;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.context.utils.Datespan;
import eu.h2020.helios_social.modules.groupcommunications.api.context.utils.Daytime;
import eu.h2020.helios_social.modules.groupcommunications.api.context.utils.Season;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumManager;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMember;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMemberRole;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumType;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.LocationForum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.SeasonalForum;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTracker;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;

import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.FORUM_DAYTIME;
import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.FORUM_DEFAULT_USER_GROUP;
import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.FORUM_END_DATE;
import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.FORUM_LAT;
import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.FORUM_LNG;
import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.FORUM_MODERATORS;
import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.FORUM_RADIUS;
import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.FORUM_ROLE;
import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.FORUM_SEASON;
import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.FORUM_START_DATE;
import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.FORUM_TAGS;
import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.GROUP_SHOW_TRUE_SELF;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FAKE_ID;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FUNNY_NAME;

public class ForumManagerImpl implements ForumManager<Transaction> {

	private final DatabaseComponent db;
	private final IdentityManager identityManager;
	private final MessageTracker messageTracker;
	private final Encoder encoder;
	private final Parser parser;
	private final Clock clock;

	@Inject
	public ForumManagerImpl(DatabaseComponent db,
			IdentityManager identityManager, MessageTracker messageTracker,
			Encoder encoder, Parser parser, Clock clock) {
		this.db = db;
		this.identityManager = identityManager;
		this.messageTracker = messageTracker;
		this.encoder = encoder;
		this.parser = parser;
		this.clock = clock;
	}

	@Override
	public void addForum(Forum forum, ForumType type, boolean isAdministrator)
			throws FormatException, DbException {
		Transaction txn = db.startTransaction(false);
		try {
			addForum(txn, forum, type, isAdministrator);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}


	@Override
	public void addForum(Transaction txn, Forum forum, ForumType type,
			boolean isAdministrator)
			throws FormatException, DbException {
		BdfList descriptor =
				BdfList.of(forum.getName(), forum.getPassword(),
						type.getValue(), isAdministrator);
		db.addGroup(txn, forum, encoder.encodeToByteArray(descriptor),
				forum.getGroupType());
		String fakeId = UUID.randomUUID().toString();
		BdfDictionary meta = BdfDictionary.of(
				new BdfEntry(FORUM_TAGS, BdfList.of(forum.getTags())),
				new BdfEntry(FORUM_DEFAULT_USER_GROUP,
						forum.getDefaultMemberRole()),
				new BdfEntry(FORUM_MODERATORS,
						BdfList.of(forum.getModerators())),
				new BdfEntry(PEER_FUNNY_NAME,
						Faker.instance().funnyName().name()),
				new BdfEntry(PEER_FAKE_ID, fakeId),
				new BdfEntry(GROUP_SHOW_TRUE_SELF, false)
		);

		if (isAdministrator) {
			meta.put(FORUM_ROLE, ForumMemberRole.ADMINISTRATOR.getInt());
			String peerId = identityManager.getIdentity().getNetworkId();
			String alias = identityManager.getIdentity().getAlias();
			db.addForumMember(txn,
					new ForumMember(new PeerId(peerId, fakeId), forum.getId(),
							alias, ForumMemberRole.ADMINISTRATOR,
							clock.currentTimeMillis()));
		} else {
			meta.put(FORUM_ROLE, forum.getDefaultMemberRole().getInt());
		}

		if (forum instanceof LocationForum) {
			meta.put(FORUM_LAT, ((LocationForum) forum).getLatitude());
			meta.put(FORUM_LNG, ((LocationForum) forum).getLongitude());
			meta.put(FORUM_RADIUS, ((LocationForum) forum).getRadius());
		} else if (forum instanceof SeasonalForum) {
			if (((SeasonalForum) forum).getSeason() instanceof Season) {
				meta.put(FORUM_SEASON,
						((Season) ((SeasonalForum) forum).getSeason())
								.getValue());
			} else if (((SeasonalForum) forum).getSeason() instanceof Daytime) {
				meta.put(FORUM_DAYTIME,
						((Daytime) ((SeasonalForum) forum).getSeason())
								.getValue());
			} else {
				meta.put(FORUM_START_DATE,
						((Datespan) ((SeasonalForum) forum).getSeason())
								.getStartDate().toEpochDay()
				);
				meta.put(FORUM_END_DATE,
						((Datespan) ((SeasonalForum) forum).getSeason())
								.getEndDate().toEpochDay()
				);
			}
		}
		db.mergeGroupMetadata(txn, forum.getId(),
				encoder.encodeMetadata(meta));
		messageTracker.initializeGroupCount(txn, forum.getId());
	}

	@Override
	public Forum getForum(String groupId) throws DbException, FormatException {
		Transaction txn = db.startTransaction(true);
		Forum forum;
		try {
			forum = getForum(txn, groupId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return forum;
	}

	@Override
	public Forum getForum(Transaction txn, String groupId)
			throws DbException, FormatException {
		Group group = db.getGroup(txn, groupId);
		Metadata metadata = db.getGroupMetadata(txn, groupId);
		return parseToForum(group, metadata);
	}

	@Override
	public Collection<Forum> getForums() throws DbException, FormatException {
		Transaction txn = db.startTransaction(true);
		Collection<Forum> forums;
		try {
			Collection<Group> groups = db.getForums(txn);
			forums = getForums(txn, groups);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return forums;
	}

	@Override
	public Collection<Forum> getForums(Transaction txn)
			throws DbException, FormatException {
		Collection<Group> groups = db.getForums(txn);
		return getForums(txn, groups);
	}

	@Override
	public Collection<Forum> getForums(String contextId)
			throws DbException, FormatException {
		Transaction txn = db.startTransaction(true);
		Collection<Forum> forums;
		try {
			Collection<Group> groups = db.getForums(txn, contextId);
			forums = getForums(txn, groups);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return forums;
	}

	@Override
	public Collection<Forum> getForums(Transaction txn, String contextId)
			throws DbException, FormatException {
		Collection<Group> groups = db.getForums(txn, contextId);
		return getForums(txn, groups);
	}

	@Override
	public Collection<ForumMember> getForumMembers(String groupId)
			throws DbException {
		Transaction txn = db.startTransaction(true);
		Collection<ForumMember> forumMembers;
		try {
			forumMembers = getForumMembers(txn, groupId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return forumMembers;
	}

	@Override
	public Collection<ForumMember> getForumMembers(Transaction txn,
			String groupId)
			throws DbException {
		return db.getForumMembers(txn, groupId);
	}

	@Override
	public ForumMemberRole getRole(String groupId)
			throws DbException, FormatException {
		Transaction txn = db.startTransaction(true);
		ForumMemberRole role;
		try {
			role = getRole(txn, groupId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return role;
	}

	@Override
	public ForumMemberRole getRole(Transaction txn, String groupId)
			throws DbException, FormatException {
		Metadata metadata = db.getGroupMetadata(txn, groupId);
		BdfDictionary meta = parser.parseMetadata(metadata);
		return ForumMemberRole.valueOf(meta.getLong(FORUM_ROLE).intValue());
	}

	@Override
	public String getFakeId(Transaction txn, String groupId)
			throws DbException, FormatException {
		Metadata metadata = db.getGroupMetadata(txn, groupId);
		BdfDictionary meta = parser.parseMetadata(metadata);
		return meta.getString(PEER_FAKE_ID);
	}

	@Override
	public void updateRole(String groupId, ForumMemberRole newRole)
			throws DbException, FormatException {
		Transaction txn = db.startTransaction(false);
		try {
			updateRole(txn, groupId, newRole);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void updateRole(Transaction txn, String groupId,
			ForumMemberRole newRole)
			throws DbException, FormatException {
		Metadata metadata = db.getGroupMetadata(txn, groupId);
		BdfDictionary meta = parser.parseMetadata(metadata);
		meta.put(FORUM_ROLE, newRole);
		db.mergeGroupMetadata(txn, groupId, encoder.encodeMetadata(meta));
	}

	@Override
	public void updateForumMemberRole(ForumMember forumMember)
			throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.updateForumMemberRole(txn, forumMember);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void updateForumMemberRole(Transaction txn, ForumMember forumMember)
			throws DbException {
		db.updateForumMemberRole(txn, forumMember);
	}

	@Override
	public void addForumMember(ForumMember forumMember)
			throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.addForumMember(txn, forumMember);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void addForumMember(Transaction txn, ForumMember forumMember)
			throws DbException {
		db.addForumMember(txn, forumMember);
	}

	@Override
	public void addForumMembers(Collection<ForumMember> forumMembers)
			throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.addForumMembers(txn, forumMembers);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void addForumMembers(Transaction txn,
			Collection<ForumMember> forumMembers)
			throws DbException {
		db.addForumMembers(txn, forumMembers);
	}

	@Override
	public void addModerator(String groupId, String moderator)
			throws DbException, FormatException {
		Transaction txn = db.startTransaction(false);
		try {
			addModerator(txn, groupId, moderator);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void addModerator(Transaction txn, String groupId,
			String moderator) throws DbException, FormatException {
		Metadata metadata = db.getGroupMetadata(txn, groupId);
		BdfDictionary meta = parser.parseMetadata(metadata);
		BdfList list = meta.getList(FORUM_MODERATORS);
		list.add(moderator);
		meta.put(FORUM_MODERATORS, list);
		db.mergeGroupMetadata(txn, groupId, encoder.encodeMetadata(meta));
	}

	@Override
	public void removeForumMember(ForumMember forumMember)
			throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.removeForumMember(txn, forumMember.getGroupId(),
					forumMember.getPeerId().getFakeId());
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void removeForumMember(Transaction txn, ForumMember forumMember)
			throws DbException {
		db.removeForumMember(txn, forumMember.getGroupId(),
				forumMember.getPeerId().getFakeId());
	}

	@Override
	public void removeModerator(String groupId, String moderator)
			throws DbException, FormatException {
		Transaction txn = db.startTransaction(false);
		try {
			removeModerator(txn, groupId, moderator);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void removeModerator(Transaction txn, String groupId,
			String moderator) throws DbException, FormatException {
		Metadata metadata = db.getGroupMetadata(txn, groupId);
		BdfDictionary meta = parser.parseMetadata(metadata);
		BdfList list = meta.getList(FORUM_MODERATORS);
		list.remove(moderator);
		meta.put(FORUM_MODERATORS, list);
		db.mergeGroupMetadata(txn, groupId, encoder.encodeMetadata(meta));
	}

	@Override
	public void removeForumMemberList(String groupId)
			throws DbException, FormatException {
		Transaction txn = db.startTransaction(false);
		try {
			db.removeForumMemberList(txn, groupId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	private Forum parseToForum(Group group, Metadata metadata)
			throws FormatException {
		BdfDictionary meta = parser.parseMetadata(metadata);
		BdfList list = parser.parseToList(group.getDescriptor());

		String name = list.getString(0);
		String password = list.getString(1);
		ForumType type = ForumType.fromValue(list.getLong(2).intValue());
		boolean isAdmin = list.getBoolean(3);

		Collection<String> moderators =
				new ArrayList(meta.getList(FORUM_MODERATORS));
		Collection<String> tags =
				new ArrayList(meta.getList(FORUM_TAGS));
		ForumMemberRole defaultRole =
				ForumMemberRole.valueOf(meta.getLong(FORUM_ROLE).intValue());

		if (type.equals(ForumType.GENERAL)) {
			return new Forum(group.getId(), group.getContextId(), name,
					password, moderators, group.getGroupType(), tags,
					defaultRole);
		} else if (type.equals(ForumType.LOCATION)) {
			return new LocationForum(group.getId(), group.getContextId(), name,
					password, moderators, group.getGroupType(), tags,
					defaultRole, meta.getDouble(FORUM_LAT),
					meta.getDouble(FORUM_LNG),
					meta.getLong(FORUM_RADIUS).intValue());
		} else {
			if (meta.getOptionalLong(FORUM_SEASON) != null) {
				return new SeasonalForum<Season>(group.getId(),
						group.getContextId(), name,
						password, moderators, group.getGroupType(), tags,
						defaultRole, Season.fromValue(
						meta.getLong(FORUM_SEASON).intValue()));
			} else if (meta.getOptionalLong(FORUM_DAYTIME) != null) {
				return new SeasonalForum<Daytime>(group.getId(),
						group.getContextId(), name,
						password, moderators, group.getGroupType(), tags,
						defaultRole, Daytime.fromValue(
						meta.getLong(FORUM_DAYTIME).intValue()));
			} else {
				return new SeasonalForum<Datespan>(group.getId(),
						group.getContextId(), name,
						password, moderators, group.getGroupType(), tags,
						defaultRole, new Datespan(
						LocalDate.ofEpochDay(
								meta.getLong(FORUM_START_DATE)),
						LocalDate.ofEpochDay(
								meta.getLong(FORUM_END_DATE)))
				);
			}
		}
	}

	private Collection<Forum> getForums(Transaction txn,
			Collection<Group> groups)
			throws FormatException, DbException {
		Collection<Forum> forums = new ArrayList();
		String[] groupsIds = new String[groups.size()];
		int i = 0;
		for (Group g : groups) {
			groupsIds[i++] = g.getId();
		}
		Map<String, Metadata> metadata = db.getGroupMetadata(txn, groupsIds);
		for (Group g : groups) {
			forums.add(parseToForum(g, metadata.get(g.getId())));
		}
		return forums;
	}
}
