package eu.h2020.helios_social.modules.groupcommunications.group.sharing;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosMessagingReceiver;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosNetworkAddress;
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

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.GROUP_INVITE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.GROUP_INVITE_RESPONSE_PROTOCOL;

public class GroupInvitationReceiver implements HeliosMessagingReceiver {

	private final GroupManager groupManager;
	private final GroupInvitationFactory groupInvitationFactory;
	private final PrivateGroupManager privateGroupManager;

	@Inject
	public GroupInvitationReceiver(GroupManager groupManager,
			PrivateGroupManager privateGroupManager,
			GroupInvitationFactory groupInvitationFactory) {
		this.groupManager = groupManager;
		this.groupInvitationFactory = groupInvitationFactory;
		this.privateGroupManager = privateGroupManager;
	}

	@Override
	public void receiveMessage(
			@NotNull HeliosNetworkAddress heliosNetworkAddress,
			@NotNull String protocolId,
			@NotNull FileDescriptor fileDescriptor) {
		if (!(protocolId.equals(GROUP_INVITE_PROTOCOL) ||
				protocolId.equals(GROUP_INVITE_RESPONSE_PROTOCOL))) return;
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
				groupManager.addGroupInvitation(groupInvitation);
			} catch (DbException e) {
				e.printStackTrace();
			}
		} else if (protocolId.equals(GROUP_INVITE_RESPONSE_PROTOCOL)) {
			ResponseInfo responseInfo =
					new Gson().fromJson(stringMessage, ResponseInfo.class);
			if (responseInfo.getGroupInvitationType()
					.equals(GroupType.PrivateGroup)) {
				if (responseInfo.getResponse()
						.equals(ResponseInfo.Response.ACCEPT)) {
					try {
						privateGroupManager
								.addMember(responseInfo.getGroupId(),
										contactId);
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
	}
}
