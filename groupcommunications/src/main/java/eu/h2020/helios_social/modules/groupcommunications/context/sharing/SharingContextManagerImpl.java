package eu.h2020.helios_social.modules.groupcommunications.context.sharing;

import com.google.gson.Gson;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import eu.h2020.helios_social.happ.helios.talk.api.context.ContextInvitationAutoResponseEvent;
import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.happ.helios.talk.api.event.EventBus;
import eu.h2020.helios_social.happ.helios.talk.api.event.EventListener;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.context.ContextType;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.SharingContextManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.context.ContextManager;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.GeneralContextProxy;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.LocationContextProxy;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.CONTEXT_INVITE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.CONTEXT_INVITE_RESPONSE_PROTOCOL;

public class SharingContextManagerImpl implements SharingContextManager,
		EventListener {

	private final ContextManager contextManager;
	private final CommunicationManager communicationManager;
	private final DatabaseComponent db;
	private final EventBus eventBus;

	@Inject
	public SharingContextManagerImpl(DatabaseComponent db,
			ContextManager contextManager,
			CommunicationManager communicationManager, EventBus eventBus) {
		this.db = db;
		this.contextManager = contextManager;
		this.communicationManager = communicationManager;
		this.eventBus = eventBus;
		this.eventBus.addListener(this);
	}

	@Override
	public void sendContextInvitation(ContextInvitation contextInvitation)
			throws DbException {
		contextManager.addPendingContext(contextInvitation);
		ContextInfo contextInfo = new ContextInfo(
				contextInvitation.getContextId(),
				contextInvitation.getName(),
				contextInvitation.getContextType(),
				contextInvitation.getJson(),
				contextInvitation.getTimestamp()
		);
		try {
			communicationManager.sendDirectMessage(CONTEXT_INVITE_PROTOCOL,
					contextInvitation.getContactId(), contextInfo);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void acceptContextInvitation(String pendingContextId)
			throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			Collection<ContextInvitation> contextInvitations =
					contextManager.getPendingContextInvitations(txn);
			Collection<ContextInvitation> filteredContextInvitations =
					contextInvitations.stream().filter(invite -> {
						return invite.getContextId()
								.equals(pendingContextId);
					}).collect(Collectors.toList());
			if (filteredContextInvitations.size() > 0) {
				ContextInvitation contextInvitation =
						filteredContextInvitations.iterator().next();
				if (contextInvitation.getContextType()
						.equals(ContextType.GENERAL)) {
					GeneralContextProxy newContext =
							new Gson().fromJson(contextInvitation.getJson(),
									GeneralContextProxy.class);
					contextManager.addContext(txn, newContext);
				} else if (contextInvitation.getContextType()
						.equals(ContextType.LOCATION)) {
					LocationContextProxy newContext =
							new Gson().fromJson(contextInvitation.getJson(),
									LocationContextProxy.class);
					contextManager.addContext(txn, newContext);
				}
			}

			for (ContextInvitation contextInvitation : filteredContextInvitations) {
				Group newContextContactGroup =
						new Group(UUID.randomUUID().toString(),
								contextInvitation.getContextId(),
								GroupType.PrivateConversation);
				db.addContactGroup(txn, newContextContactGroup,
						contextInvitation.getContactId());
				ConnectionInfo connectionInfo = new ConnectionInfo()
						.setConversationInfo(newContextContactGroup.getId(),
								newContextContactGroup.getContextId());
				try {
					communicationManager
							.sendDirectMessage(CONTEXT_INVITE_RESPONSE_PROTOCOL,
									contextInvitation.getContactId(),
									connectionInfo);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				} catch (TimeoutException e) {
					e.printStackTrace();
				}
			}
			db.removePendingContext(txn, pendingContextId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void acceptContextInvitation(ContextInvitation contextInvitation)
			throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			Group newContextContactGroup =
					new Group(UUID.randomUUID().toString(),
							contextInvitation.getContextId(),
							GroupType.PrivateConversation);
			db.addContactGroup(txn, newContextContactGroup,
					contextInvitation.getContactId());
			ConnectionInfo connectionInfo = new ConnectionInfo()
					.setConversationInfo(newContextContactGroup.getId(),
							newContextContactGroup.getContextId());
			try {
				communicationManager
						.sendDirectMessage(CONTEXT_INVITE_RESPONSE_PROTOCOL,
								contextInvitation.getContactId(),
								connectionInfo);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void rejectContextInvitation(ContextInvitation contextInvitation)
			throws DbException {
		contextManager.removePendingContext(contextInvitation.getContextId());
		try {
			communicationManager
					.sendDirectMessage(CONTEXT_INVITE_RESPONSE_PROTOCOL,
							contextInvitation.getContactId(),
							new ConnectionInfo()
									.setConversationInfo(null,
											contextInvitation.getContextId()));
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof ContextInvitationAutoResponseEvent) {
			try {
				acceptContextInvitation(((ContextInvitationAutoResponseEvent) e)
						.getContextInvitation());
			} catch (DbException ex) {
				ex.printStackTrace();
			}
		}
	}
}
