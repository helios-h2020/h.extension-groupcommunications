package eu.h2020.helios_social.modules.groupcommunications.group;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupMember;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfDictionary;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.Encoder;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.Parser;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Metadata;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications_utils.identity.IdentityManager;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMemberRole;
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
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.JoinGroupEvent;

import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.GROUP_SHOW_TRUE_SELF;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FAKE_ID;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FUNNY_NAME;

public class GroupManagerImpl implements GroupManager<Transaction> {

    private final PrivateGroupManager privateGroupManager;
    private final ForumManager forumManager;
    private final DatabaseComponent db;
    private final IdentityManager identityManager;
    private final Parser parser;
    private final Encoder encoder;
    private final EventBus eventBus;

    @Inject
    public GroupManagerImpl(DatabaseComponent db,
                            PrivateGroupManager privateGroupManager, ForumManager forumManager,
                            IdentityManager identityManager, EventBus eventBus, Parser parse, Encoder encoder) {
        this.db = db;
        this.privateGroupManager = privateGroupManager;
        this.identityManager = identityManager;
        this.parser = parse;
        this.forumManager = forumManager;
        this.encoder = encoder;
        this.eventBus = eventBus;
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
                PeerId pid = new PeerId(identityManager.getIdentity().getId());
                GroupMember groupMember = new GroupMember(pid,
                        identityManager.getIdentity().getAlias(),
                        identityManager.getIdentity().getProfilePicture(),
                        group.getId());
                privateGroupManager.addMember(txn,groupMember);
                eventBus.broadcast(new JoinGroupEvent(group.getId(), ((PrivateGroup) group).getPassword(), group.getGroupType()));
            } else if (group instanceof LocationForum) {
                forumManager.addForum((LocationForum) group, ForumType.LOCATION, true);
                eventBus.broadcast(new JoinGroupEvent(group.getId(), ((LocationForum) group).getPassword(), group.getGroupType()));
            } else if (group instanceof SeasonalForum) {
                forumManager.addForum((SeasonalForum) group, ForumType.SEASONAL, true);
                eventBus.broadcast(new JoinGroupEvent(group.getId(), ((SeasonalForum) group).getPassword(), group.getGroupType()));
            } else {
                forumManager.addForum((Forum) group, ForumType.GENERAL, true);
                eventBus.broadcast(new JoinGroupEvent(group.getId(), ((Forum) group).getPassword(), group.getGroupType()));
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
    public GroupType getGroupType(String groupId) throws DbException {
        Transaction txn = db.startTransaction(true);
        GroupType groupType;
        try {
            groupType = db.getGroup(txn, groupId).getGroupType();
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return groupType;
    }

    @Override
    public boolean groupAlreadyExists(String groupId) throws DbException {
        Transaction txn = db.startTransaction(true);
        boolean exists;
        try {
            exists = db.groupAlreadyExists(txn, groupId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return exists;
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
    public Collection<Forum> getForums(String contextId)
            throws DbException, FormatException {
        return forumManager.getForums(contextId);
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
    public void revealSelf(String groupId, boolean doReveal) throws DbException, FormatException {
        Transaction txn = db.startTransaction(false);
        try {
            revealSelf(txn, groupId, doReveal);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public void revealSelf(Transaction txn, String groupId, boolean doReveal) throws DbException,
            FormatException {
        Metadata metadata = db.getGroupMetadata(txn, groupId);
        BdfDictionary meta = parser.parseMetadata(metadata);
        if (!meta.getBoolean(GROUP_SHOW_TRUE_SELF) && doReveal) {
            meta.put(GROUP_SHOW_TRUE_SELF, true);
            db.mergeGroupMetadata(txn, groupId, encoder.encodeMetadata(meta));
        } else if (meta.getBoolean(GROUP_SHOW_TRUE_SELF) && !doReveal) {
            meta.put(GROUP_SHOW_TRUE_SELF, false);
            db.mergeGroupMetadata(txn, groupId, encoder.encodeMetadata(meta));
        }
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
    public int pendingIncomingGroupInvitations()
            throws DbException {
        Transaction txn = db.startTransaction(true);
        int pendingGroupInvitations = 0;
        try {
            pendingGroupInvitations = db.countPendingGroupInvitations(txn, true);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return pendingGroupInvitations;
    }

    @Override
    public int pendingOutgoingGroupInvitations()
            throws DbException {
        Transaction txn = db.startTransaction(true);
        int pendingGroupInvitations = 0;
        try {
            pendingGroupInvitations = db.countPendingGroupInvitations(txn, false);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return pendingGroupInvitations;
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

    @Override
    public boolean identityRevealed(String groupId) throws DbException, FormatException {
        Transaction txn = db.startTransaction(true);
        boolean isRevealed;
        try {
            Metadata metadata = db.getGroupMetadata(txn, groupId);
            BdfDictionary meta = parser.parseMetadata(metadata);
            isRevealed = meta.getBoolean(GROUP_SHOW_TRUE_SELF);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return isRevealed;
    }

    @Override
    public ForumMemberRole getRole(Forum forum) throws DbException, FormatException {
        return forumManager.getRole(forum.getId());
    }

    @Override
    public ForumMemberRole getRole(Transaction txn, Forum forum) throws DbException,
            FormatException {
        return forumManager.getRole(txn, forum.getId());
    }

}
