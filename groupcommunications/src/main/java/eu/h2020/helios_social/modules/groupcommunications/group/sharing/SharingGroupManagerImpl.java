package eu.h2020.helios_social.modules.groupcommunications.group.sharing;

import com.google.gson.Gson;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.utils.Pair;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.Event;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventListener;
import eu.h2020.helios_social.modules.groupcommunications_utils.identity.Identity;
import eu.h2020.helios_social.modules.groupcommunications_utils.identity.IdentityManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.lifecycle.IoExecutor;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.GroupInvitationAutoAcceptEvent;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.JoinGroupEvent;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.LeaveGroupEvent;
import eu.h2020.helios_social.modules.groupcommunications_utils.system.Clock;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.LocationForum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.SeasonalForum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.membership.MembershipInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupManager;
import eu.h2020.helios_social.modules.groupcommunications.api.group.sharing.GroupInvitationType;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.ResponseInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.group.sharing.SharingGroupManager;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.FORUM_MEMBERSHIP_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.GROUP_INVITE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.GROUP_INVITE_RESPONSE_PROTOCOL;
import static java.util.logging.Logger.getLogger;

public class SharingGroupManagerImpl implements SharingGroupManager,
        EventListener {
    private final Logger LOG =
            getLogger(
                    SharingGroupManagerImpl.class.getName());

    private final GroupManager groupManager;
    private final CommunicationManager communicationManager;
    private final IdentityManager identityManager;
    private final DatabaseComponent db;
    private final Executor ioExecutor;
    private final Clock clock;

    @Inject
    public SharingGroupManagerImpl(DatabaseComponent db,
                                   @IoExecutor Executor ioExecutor,
                                   GroupManager groupManager, IdentityManager identityManager,
                                   CommunicationManager communicationManager,
                                   Clock clock) {
        this.db = db;
        this.ioExecutor = ioExecutor;
        this.groupManager = groupManager;
        this.identityManager = identityManager;
        this.clock = clock;
        this.communicationManager = communicationManager;
    }

    @Override
    public void sendGroupInvitation(GroupInvitation groupInvitation)
            throws DbException {
        GroupInfo groupInfo = new GroupInfo(
                groupInvitation.getContextId(),
                groupInvitation.getGroupId(),
                groupInvitation.getName(),
                groupInvitation.getGroupInvitationType(),
                groupInvitation.getJson(),
                groupInvitation.getTimestamp()
        );
        groupManager.addGroupInvitation(groupInvitation);
        sendMessage(GROUP_INVITE_PROTOCOL,
                    groupInvitation.getContactId(),
                    groupInfo);
    }

    @Override
    public void acceptGroupInvitation(GroupInvitation groupInvitation)
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(false);
        try {
            if (groupInvitation.getGroupInvitationType()
                    .equals(GroupInvitationType.PrivateGroup)) {
                LOG.info("Accepting Group Invite");
                PrivateGroup privateGroup = new Gson()
                        .fromJson(groupInvitation.getJson(),
                                  PrivateGroup.class);
                groupManager.addGroup(txn, privateGroup);

                db.removeGroupInvitation(txn, groupInvitation.getContactId(),
                                         groupInvitation.getGroupId());

                communicationManager
                        .subscribe(privateGroup.getId(),
                                   privateGroup.getPassword());

                sendMessage(GROUP_INVITE_RESPONSE_PROTOCOL,
                            groupInvitation.getContactId(),
                            new ResponseInfo(
                                    ResponseInfo.Response.ACCEPT,
                                    groupInvitation.getGroupId(),
                                    groupInvitation
                                            .getGroupInvitationType()));
            } else if (groupInvitation.getGroupInvitationType().getValue() >= 1) {
                Forum forum;
                if (groupInvitation.getGroupInvitationType().equals(GroupInvitationType.LocationForum))
                    forum = new Gson().fromJson(groupInvitation.getJson(), LocationForum.class);
                else if (groupInvitation.getGroupInvitationType().equals(GroupInvitationType.SeasonalForum))
                    forum = new Gson().fromJson(groupInvitation.getJson(), SeasonalForum.class);
                else
                    forum = new Gson().fromJson(groupInvitation.getJson(), Forum.class);

                LOG.info("Accepting Forum Invite");
                groupManager.addGroup(txn, forum);

                db.removeGroupInvitation(txn, groupInvitation.getContactId(),
                                         groupInvitation.getGroupId());
                communicationManager.subscribe(
                        forum.getId(),
                        forum.getPassword());

                sendMessage(GROUP_INVITE_RESPONSE_PROTOCOL,
                            groupInvitation.getContactId(),
                            new ResponseInfo(
                                    ResponseInfo.Response.ACCEPT,
                                    groupInvitation.getGroupId(),
                                    groupInvitation.getGroupInvitationType())
                );
                notifyModeratorsForJoiningForum(txn, forum);
            }
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public void joinForum(Forum forum) throws DbException, FormatException {
        Transaction txn = db.startTransaction(false);
        try {
            groupManager.addGroup(txn, forum);
            communicationManager.subscribe(
                    forum.getId(),
                    forum.getPassword());
            notifyModeratorsForJoiningForum(txn, forum);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public void rejectGroupInvitation(GroupInvitation groupInvitation)
            throws DbException {
        groupManager.removeGroupInvitation(groupInvitation.getContactId(),
                                           groupInvitation.getGroupId());

        sendMessage(GROUP_INVITE_RESPONSE_PROTOCOL,
                    groupInvitation.getContactId(), new ResponseInfo(
                        ResponseInfo.Response.REJECT,
                        groupInvitation.getGroupId(),
                        groupInvitation.getGroupInvitationType()));
    }

    @Override
    public void eventOccurred(Event e) {
        if (e instanceof JoinGroupEvent) {
            communicationManager.subscribe(
                    ((JoinGroupEvent) e).getGroupId(),
                    ((JoinGroupEvent) e).getPassword());
        } else if (e instanceof LeaveGroupEvent) {
            try {
                if (((LeaveGroupEvent) e).getGroupType()
                        .equals(GroupType.PrivateGroup)) {
                    //TODO: Missing parts of the implementation
                    PrivateGroup group = (PrivateGroup) groupManager
                            .getGroup(((LeaveGroupEvent) e).getGroupId(),
                                      ((LeaveGroupEvent) e).getGroupType());
                    communicationManager
                            .unsubscribe(group.getId(),
                                         group.getPassword());
                }
            } catch (FormatException ex) {
                ex.printStackTrace();
            } catch (DbException ex) {
                ex.printStackTrace();
            }
        } else if (e instanceof GroupInvitationAutoAcceptEvent) {
            try {
                acceptGroupInvitation(((GroupInvitationAutoAcceptEvent) e).getGroupInvitation());
            } catch (DbException dbException) {
                dbException.printStackTrace();
            } catch (FormatException formatException) {
                formatException.printStackTrace();
            }
        }
    }

    private void notifyModeratorsForJoiningForum(Transaction txn, Forum forum)
            throws DbException, FormatException {
        Identity identity = identityManager.getIdentity();
        Pair<String, String> fakeIdentity = groupManager.getFakeIdentity(
                txn,
                forum.getId());
        String fakeId = fakeIdentity.getSecond();
        MembershipInfo membershipInfo =
                new MembershipInfo(
                        forum.getId(),
                        new PeerId(identity.getNetworkId(), fakeId),
                        forum.getDefaultMemberRole(),
                        identity.getAlias(),
                        fakeIdentity.getFirst(),
                        clock.currentTimeMillis());
        membershipInfo.setAction(MembershipInfo.Action.JOIN_FORUM);
        for (String moderator : (List<String>) forum.getModerators()) {
            notify(new PeerId(moderator), membershipInfo);
        }
    }

    private void notify(PeerId peerId, MembershipInfo membershipInfo) {
        LOG.info("Notifying moderator for joining forum! ");
        ioExecutor.execute(() -> {

            communicationManager.sendDirectMessage(
                    FORUM_MEMBERSHIP_PROTOCOL,
                    peerId,
                    membershipInfo);

        });
    }

    private void sendMessage(String protocol, ContactId contactId, AbstractMessage message) {
        ioExecutor.execute(() -> {
            communicationManager.sendDirectMessage(
                    protocol,
                    contactId,
                    message
            );
        });
    }
}
