package eu.h2020.helios_social.modules.groupcommunications.group.sharing;

import android.util.Log;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import javax.inject.Inject;

import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosMessagingReceiver;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosNetworkAddress;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.Contact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupMember;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroupMemberListInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroupNewMemberInfo;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.GroupInvitationAutoAcceptEvent;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupManager;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroupManager;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitationFactory;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.ResponseInfo;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.GroupMemberListUpdateEvent;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.GROUP_INVITE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.GROUP_INVITE_RESPONSE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.GROUP_MEMBER_LIST_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.NEW_GROUP_MEMBER_PROTOCOL;

public class GroupInvitationReceiver implements HeliosMessagingReceiver {

    private final GroupManager groupManager;
    private final GroupInvitationFactory groupInvitationFactory;
    private final PrivateGroupManager privateGroupManager;
    private final EventBus eventBus;
    private final ContactManager contactManager;

    @Inject
    public GroupInvitationReceiver(GroupManager groupManager,
                                   PrivateGroupManager privateGroupManager,
                                   GroupInvitationFactory groupInvitationFactory,
                                   EventBus eventBus,
                                   ContactManager contactManager) {
        this.groupManager = groupManager;
        this.groupInvitationFactory = groupInvitationFactory;
        this.privateGroupManager = privateGroupManager;
        this.eventBus = eventBus;
        this.contactManager = contactManager;
    }

    @Override
    public void receiveMessage(
            @NotNull HeliosNetworkAddress heliosNetworkAddress,
            @NotNull String protocolId,
            @NotNull FileDescriptor fileDescriptor) {
        System.out.println("received message: " + GROUP_MEMBER_LIST_PROTOCOL);

        if (!(protocolId.equals(GROUP_INVITE_PROTOCOL) ||
                protocolId.equals(GROUP_INVITE_RESPONSE_PROTOCOL) ||
                protocolId.equals(NEW_GROUP_MEMBER_PROTOCOL) ||
                protocolId.equals(GROUP_MEMBER_LIST_PROTOCOL))) return;

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
        String stringMessage = new String(data, StandardCharsets.UTF_8);
        System.out.println("received message: " + stringMessage);
        ContactId contactId =
                new ContactId(heliosNetworkAddress.getNetworkId());


        if (protocolId.equals(GROUP_INVITE_PROTOCOL)) {
            GroupInfo groupInfo =
                    new Gson().fromJson(stringMessage, GroupInfo.class);
            GroupInvitation groupInvitation = groupInvitationFactory
                    .createIncomingGroupInvitation(contactId, groupInfo);
            try {
                if (groupManager.groupAlreadyExists(groupInvitation.getGroupId())) {
                    eventBus.broadcast(new GroupInvitationAutoAcceptEvent(groupInvitation));
                } else groupManager.addGroupInvitation(groupInvitation);
            } catch (DbException e) {
                e.printStackTrace();
            }
        } else if (protocolId.equals(GROUP_INVITE_RESPONSE_PROTOCOL)) {
            ResponseInfo responseInfo =
                    new Gson().fromJson(stringMessage, ResponseInfo.class);
            Log.d("getGroupInvitationType", String.valueOf(responseInfo.getGroupInvitationType()));
            Log.d("getResponse", String.valueOf(responseInfo.getResponse()));

            if (String.valueOf(responseInfo.getGroupInvitationType())
                    .equals("PrivateGroup")) {
                if (String.valueOf(responseInfo.getResponse())
                        .equals("ACCEPT")) {
                    try {

                        // add the new member to group
                        Contact contact = null;
                        try {
                            contact = contactManager.getContact(contactId);
                            PeerId pid = new PeerId(contact.getId().getId());
                            Log.d("peerIdSend", pid.getId());

                            GroupMember groupMember = new GroupMember(pid,
                                    contact.getAlias(),
                                    contact.getProfilePicture(),
                                    responseInfo.getGroupId());
                            privateGroupManager.addMember(groupMember);
                            // send the new member list to group members
                            Collection<GroupMember> groupMembers = privateGroupManager.getMembers(responseInfo.getGroupId());
                            // this event is employed by SharingGroupManagerImpl
                            eventBus.broadcast(new GroupMemberListUpdateEvent(groupMembers,pid.getId(),responseInfo.getGroupId()));
                        } catch (DbException e) {
                            e.printStackTrace();
                        }


                        groupManager.removeGroupInvitation(contactId,
                                responseInfo.getGroupId());
                    } catch (DbException e) {
                        e.printStackTrace();
                    } catch (FormatException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        groupManager.removeGroupInvitation(contactId,
                                responseInfo.getGroupId());
                    } catch (DbException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    groupManager.removeGroupInvitation(contactId,
                            responseInfo.getGroupId());
                } catch (DbException e) {
                    e.printStackTrace();
                }
            }
        }
        else if (protocolId.equals(NEW_GROUP_MEMBER_PROTOCOL)){
            PrivateGroupNewMemberInfo privateGroupNewMemberInfo =
                    new Gson().fromJson(stringMessage, PrivateGroupNewMemberInfo.class);
            // add the new member to group
            try {
                PeerId pid = new PeerId(privateGroupNewMemberInfo.getPeerId());
                GroupMember groupMember = new GroupMember(pid,
                        privateGroupNewMemberInfo.getAlias(),
                        privateGroupNewMemberInfo.getProfilePicture(),
                        privateGroupNewMemberInfo.getGroupId());
                privateGroupManager.addMember(groupMember);

            } catch (DbException e) {
                e.printStackTrace();
            } catch (FormatException e) {
                e.printStackTrace();
            }
        }
        else if (protocolId.equals(GROUP_MEMBER_LIST_PROTOCOL)){
            PrivateGroupMemberListInfo privateGroupMemberListInfo =
                    new Gson().fromJson(stringMessage, PrivateGroupMemberListInfo.class);
            Collection<GroupMember> groupMembers = privateGroupMemberListInfo.getGroupMembers();
            for (GroupMember m: groupMembers){
                try {
                    privateGroupManager.addMember(m);
                } catch (DbException e) {
                    e.printStackTrace();
                } catch (FormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
