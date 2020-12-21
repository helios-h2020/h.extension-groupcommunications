package eu.h2020.helios_social.modules.groupcommunications.context.sharing;

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
import eu.h2020.helios_social.happ.helios.talk.api.context.ContextInvitationAutoResponseEvent;
import eu.h2020.helios_social.happ.helios.talk.api.event.EventBus;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitationFactory;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.conversation.ConversationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.context.ContextManager;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.CONTEXT_INVITE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.CONTEXT_INVITE_RESPONSE_PROTOCOL;

public class ContextInvitationReceiver implements HeliosMessagingReceiver {

	private final ContextManager contextManager;
	private final ConversationManager conversationManager;
	private final ContextInvitationFactory contextInvitationFactory;
	private final EventBus eventBus;

	@Inject
	public ContextInvitationReceiver(ContextManager contextManager,
			ConversationManager conversationManager,
			ContextInvitationFactory contextInvitationFactory,
			EventBus eventBus) {
		this.contextManager = contextManager;
		this.conversationManager = conversationManager;
		this.contextInvitationFactory = contextInvitationFactory;
		this.eventBus = eventBus;
	}

	@Override
	public void receiveMessage(
			@NotNull HeliosNetworkAddress heliosNetworkAddress,
			@NotNull String protocolId,
			@NotNull FileDescriptor fileDescriptor) {
		if (!(protocolId.equals(CONTEXT_INVITE_PROTOCOL) ||
				protocolId.equals(CONTEXT_INVITE_RESPONSE_PROTOCOL))) return;
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
		ContactId contactId =
				new ContactId(heliosNetworkAddress.getNetworkId());
		if (protocolId.equals(CONTEXT_INVITE_PROTOCOL)) {
			ContextInfo contextInfo =
					new Gson().fromJson(stringMessage, ContextInfo.class);
			ContextInvitation contextInvitation = contextInvitationFactory
					.createIncomingContextInvitation(contactId, contextInfo);
			try {
				if (contextManager
						.contextExists(contextInvitation.getContextId())) {
					eventBus.broadcast(
							new ContextInvitationAutoResponseEvent(
									contextInvitation));
				} else {
					contextManager.addPendingContext(contextInvitation);
				}
			} catch (DbException e) {
				e.printStackTrace();
			}
		} else if (protocolId.equals(CONTEXT_INVITE_RESPONSE_PROTOCOL)) {
			ConnectionInfo connectionInfo =
					new Gson().fromJson(stringMessage, ConnectionInfo.class);
			try {
				if (connectionInfo.getGroupId() != null) {
					Group group = new Group(connectionInfo.getGroupId(),
							connectionInfo.getContextId(),
							GroupType.PrivateConversation);
					conversationManager.addContactGroup(contactId, group);
				}

				contextManager.removeContextInvitation(
						contactId,
						connectionInfo.getContextId());
			} catch (DbException e) {
				e.printStackTrace();
			}
		}
	}
}
