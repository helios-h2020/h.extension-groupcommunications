package eu.h2020.helios_social.modules.groupcommunications.forum.mebership;

import java.lang.reflect.Member;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.happ.helios.talk.api.identity.Identity;
import eu.h2020.helios_social.happ.helios.talk.api.identity.IdentityManager;
import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.IoExecutor;
import eu.h2020.helios_social.happ.helios.talk.api.system.Clock;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumManager;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMember;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMemberRole;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.membership.ForumMembershipManager;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.membership.MembershipInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.FORUM_MEMBERSHIP_PROTOCOL;

public class ForumMembershipManagerImpl implements ForumMembershipManager {

    private final DatabaseComponent db;
    private final ForumManager forumManager;
    private final CommunicationManager communicationManager;
    private final IdentityManager identityManager;
    private final Clock clock;
    private final Executor ioExecutor;

    @Inject
    public ForumMembershipManagerImpl(@IoExecutor Executor ioExecutor,
                                      DatabaseComponent db,
                                      IdentityManager identityManager,
                                      ForumManager forumManager,
                                      CommunicationManager communicationManager, Clock clock) {
        this.db = db;
        this.ioExecutor = ioExecutor;
        this.identityManager = identityManager;
        this.forumManager = forumManager;
        this.communicationManager = communicationManager;
        this.clock = clock;
    }

    /**
     * Removes forum from the database and the corresponding list if exists and notifies the
     * administrator(s) and moderator(s) to remove her from their forum member list. If the user
     * is an administrator or moderator notifies all forum members to remove her from the
     * moderator list.
     *
     * @param forum
     * @throws DbException
     * @throws FormatException
     */
    @Override
    public void leaveForum(Forum forum) throws DbException, FormatException {
        Transaction txn = db.startTransaction(false);
        try {
            ForumMemberRole personalRole =
                    forumManager.getRole(txn, forum.getId());
            Identity identity = identityManager.getIdentity();
            MembershipInfo membershipInfo =
                    new MembershipInfo(
                            forum.getId(),
                            new PeerId(identity.getNetworkId()),
                            null,
                            null,
                            clock.currentTimeMillis());
            if (personalRole == ForumMemberRole.MODERATOR ||
                    personalRole == ForumMemberRole.ADMINISTRATOR) {
                Collection<ForumMember> forumMembers =
                        forumManager.getForumMembers(txn, forum.getId());

                for (ForumMember member : forumMembers) {
                    if (member.getPeerId().getId()
                            .equals(identity.getNetworkId())) continue;
                    if (member.getRole() == ForumMemberRole.ADMINISTRATOR ||
                            member.getRole() == ForumMemberRole.MODERATOR)
                        membershipInfo.setAction(
                                MembershipInfo.Action.LEAVE_FORUM);
                    else
                        membershipInfo.setAction(
                                MembershipInfo.Action.REMOVE_MODERATOR);
                    notify(member.getPeerId(), membershipInfo);
                }
            } else {
                membershipInfo.setAction(MembershipInfo.Action.LEAVE_FORUM);
                for (String moderator : forum.getModerators()) {
                    notify(new PeerId(moderator), membershipInfo);
                }
            }
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    /**
     * Updates the role of a Forum Member if she is Administator/Moderator of the Forum and
     * notifies all the moderators and administrators to update accordingly the member list
     * retained in their database.
     *
     * @param forum
     * @param forumMember
     * @param updatedRole
     * @throws DbException
     * @throws FormatException
     */
    @Override
    public void updateForumMemberRole(Forum forum, ForumMember forumMember,
                                      ForumMemberRole updatedRole) throws DbException,
            FormatException {
        Transaction txn = db.startTransaction(false);
        try {
            ForumMemberRole personalRole =
                    forumManager.getRole(forumMember.getGroupId());
            //only MODERATORS and ADMINISTRATORS have access to member lists and can
            // change roles of forum members, additionally if updated Role equals to
            // previous no changes are performed to the member lists
            if (!personalRole.equals(ForumMemberRole.MODERATOR) ||
                    !personalRole.equals(ForumMemberRole.ADMINISTRATOR) ||
                    forumMember.getRole().equals(updatedRole)) {
                return;
            }
            //only ADMINISTRATORS can change the role on a MODERATOR
            if (personalRole.equals(ForumMemberRole.MODERATOR) &&
                    forumMember.getRole().equals(ForumMemberRole.MODERATOR)) {
                return;
            }

            MembershipInfo membershipInfo =
                    new MembershipInfo(
                            forumMember.getGroupId(),
                            forumMember.getPeerId(),
                            updatedRole,
                            null,
                            clock.currentTimeMillis()
                    );
            long timestamp = clock.currentTimeMillis();
            db.updateForumMemberRole(txn, forumMember.getGroupId(),
                    forumMember.getPeerId().getFakeId(), updatedRole,
                    timestamp);

            if ((updatedRole == ForumMemberRole.MODERATOR &&
                    forumMember.getRole() != ForumMemberRole.ADMINISTRATOR) ||
                    (updatedRole == ForumMemberRole.ADMINISTRATOR &&
                            forumMember.getRole() !=
                                    ForumMemberRole.MODERATOR)) {
                //notify members to add moderator
                Collection<ForumMember> forumMembers =
                        forumManager.getForumMembers(txn, forum.getId());

                for (ForumMember member : forumMembers) {
                    if (member.getPeerId().getId()
                            .equals(forumMember.getPeerId().getId())) continue;
                    membershipInfo
                            .setAction(MembershipInfo.Action.ADD_MODERATOR);
                    notify(member.getPeerId(), membershipInfo);
                }
                membershipInfo.setForumMemberList(forumMembers);
                notify(forumMember.getPeerId(), membershipInfo);
            } else if (forumMember.getRole() == ForumMemberRole.MODERATOR) {
                forumManager.removeModerator(txn, forum.getId(),
                        forumMember.getPeerId().getId());
                Collection<ForumMember> forumMembers =
                        forumManager.getForumMembers(txn, forum.getId());
                //notify members to remove moderator
                membershipInfo
                        .setAction(MembershipInfo.Action.REMOVE_MODERATOR);
                for (ForumMember member : forumMembers) {
                    notify(member.getPeerId(), membershipInfo);
                }
            } else {
                Collection<String> moderators = forum.getModerators();

                //notify moderators for the change
                membershipInfo.setAction(MembershipInfo.Action.UPDATE_ROLE);
                for (String moderator : moderators) {
                    notify(new PeerId(moderator), membershipInfo);
                }

            }
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }

    }

    private void notify(PeerId peerId, MembershipInfo membershipInfo) {
        ioExecutor.execute(() -> {
            try {
                communicationManager.sendDirectMessage(
                        FORUM_MEMBERSHIP_PROTOCOL,
                        peerId,
                        membershipInfo);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        });
    }
}
