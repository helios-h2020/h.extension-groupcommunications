package eu.h2020.helios_social.modules.groupcommunications.forum.mebership;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.core.messaging.HeliosMessagingReceiver;
import eu.h2020.helios_social.core.messaging.HeliosNetworkAddress;
import eu.h2020.helios_social.modules.groupcommunications_utils.identity.Identity;
import eu.h2020.helios_social.modules.groupcommunications_utils.identity.IdentityManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumManager;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMember;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMemberRole;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.membership.MembershipInfo;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.FORUM_MEMBERSHIP_PROTOCOL;
import static java.util.logging.Logger.getLogger;

public class MembershipReceiver
        implements HeliosMessagingReceiver {
    private final Logger LOG = getLogger(MembershipReceiver.class.getName());

    private ForumManager forumManager;
    private IdentityManager identityManager;

    @Inject
    public MembershipReceiver(ForumManager forumManager,
                              IdentityManager identityManager) {
        this.forumManager = forumManager;
        this.identityManager = identityManager;
    }

    @Override
    public void receiveMessage(
            @NotNull HeliosNetworkAddress heliosNetworkAddress,
            @NotNull String protocolId,
            @NotNull FileDescriptor fileDescriptor) {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        try (FileInputStream fileInputStream = new FileInputStream(
                fileDescriptor)) {
            int byteRead;
            while ((byteRead = fileInputStream.read()) != -1) {
                ba.write(byteRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        receiveMessage(heliosNetworkAddress, protocolId, ba.toByteArray());
    }

    @Override
    public void receiveMessage(
            @NotNull HeliosNetworkAddress heliosNetworkAddress,
            @NotNull String protocolId, @NotNull byte[] data) {
        if (!protocolId.equals(FORUM_MEMBERSHIP_PROTOCOL)) return;
        String stringMessage = new String(data, StandardCharsets.UTF_8);

        MembershipInfo membershipInfo = new Gson()
                .fromJson(stringMessage, MembershipInfo.class);
        LOG.info("MembershipInfo Received!");

        try {
            ForumMemberRole role =
                    forumManager.getRole(membershipInfo.getGroupId());

            ForumMember forumMember = new ForumMember(
                    membershipInfo.getPeerId(),
                    membershipInfo.getGroupId(),
                    membershipInfo.getAlias(),
                    membershipInfo.getFakeName(),
                    membershipInfo.getRole(),
                    membershipInfo.getTimestamp());
            Identity identity = identityManager.getIdentity();

            if (role == ForumMemberRole.MODERATOR ||
                    role == ForumMemberRole.ADMINISTRATOR) {

                if (membershipInfo.getAction() == MembershipInfo.Action.JOIN_FORUM) {
                    forumManager.addForumMember(forumMember);
                } else if (membershipInfo.getAction() == MembershipInfo.Action.LEAVE_FORUM) {
                    if (forumMember.getRole() == ForumMemberRole.MODERATOR ||
                            forumMember.getRole() == ForumMemberRole.ADMINISTRATOR) {
                        forumManager.removeModerator(
                                forumMember.getGroupId(),
                                forumMember.getPeerId().getId());
                    }
                    forumManager.removeForumMember(forumMember);
                } else if (membershipInfo.getAction() ==
                        MembershipInfo.Action.UPDATE_ROLE) {
                    LOG.info("Updating Forum's Member Role");
                    forumManager.updateForumMemberRole(forumMember);
                }
            } else {
                Forum forum = forumManager.getForum(membershipInfo.getGroupId());
                if (!forum.getModerators().contains(heliosNetworkAddress.getNetworkId())) return;

                if (membershipInfo.getAction() == MembershipInfo.Action.ADD_MODERATOR) {
                    LOG.info("Adding a new forum moderator!");
                    forumManager.addModerator(
                            membershipInfo.getGroupId(),
                            membershipInfo.getPeerId().getId()
                    );
                    if (forumMember.getPeerId().getId().equals(identity.getNetworkId())) {
                        LOG.info("Your role in forum " + forum.getName() + " has been updated! You have been added as moderator");
                        forumManager.updateRole(membershipInfo.getGroupId(), membershipInfo.getRole());
                        forumManager.addForumMembers(
                                membershipInfo.getForumMemberList());
                    }
                } else if (membershipInfo.getAction() ==
                        MembershipInfo.Action.REMOVE_MODERATOR) {
                    forumManager.removeModerator(
                            membershipInfo.getGroupId(),
                            membershipInfo.getPeerId().getId());
                    if (forumMember.getPeerId().getId()
                            .equals(identity.getNetworkId())) {
                        LOG.info("Your role in forum " + forum.getName() + " has been updated! You have been removed from moderators");
                        forumManager.updateRole(membershipInfo.getGroupId(), membershipInfo.getRole());
                        forumManager.removeForumMemberList(
                                membershipInfo.getGroupId());
                    }
                } else if (membershipInfo.getAction() == MembershipInfo.Action.UPDATE_ROLE &&
                        forumMember.getPeerId().getId().equals(identity.getNetworkId())) {
                    LOG.info("Your role in forum " + forum.getName() + " has been updated!");
                    forumManager.updateRole(
                            forumMember.getGroupId(),
                            forumMember.getRole()
                    );
                }
            }
        } catch (DbException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
    }
}
