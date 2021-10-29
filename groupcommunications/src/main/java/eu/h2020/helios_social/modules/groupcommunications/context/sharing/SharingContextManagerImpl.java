package eu.h2020.helios_social.modules.groupcommunications.context.sharing;

import android.util.Log;

import com.google.gson.Gson;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.SpatioTemporalContext;
import eu.h2020.helios_social.modules.groupcommunications_utils.lifecycle.IoExecutor;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.ContextInvitationAutoResponseEvent;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.Event;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventListener;
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
    private final Executor ioExecutor;
    private final DatabaseComponent db;

    @Inject
    public SharingContextManagerImpl(DatabaseComponent db,
                                     @IoExecutor Executor ioExecutor,
                                     ContextManager contextManager,
                                     CommunicationManager communicationManager) {
        this.db = db;
        this.ioExecutor = ioExecutor;
        this.contextManager = contextManager;
        this.communicationManager = communicationManager;
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
        sendMessage(CONTEXT_INVITE_PROTOCOL, contextInvitation.getContactId(), contextInfo);
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
                    Log.d("contextPrivateName: ",newContext.getPrivateName());
                    contextManager.addContext(txn, newContext);
                } else if (contextInvitation.getContextType()
                        .equals(ContextType.LOCATION)) {
                    LocationContextProxy newContext =
                            new Gson().fromJson(contextInvitation.getJson(),
                                                LocationContextProxy.class);
                    contextManager.addContext(txn, newContext);
                } else if (contextInvitation.getContextType()
                        .equals(ContextType.SPATIOTEMPORAL)) {
                    SpatioTemporalContext newContext =
                            new Gson().fromJson(contextInvitation.getJson(),
                                    SpatioTemporalContext.class);
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

                sendMessage(CONTEXT_INVITE_RESPONSE_PROTOCOL,
                            contextInvitation.getContactId(),
                            connectionInfo);
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

            sendMessage(CONTEXT_INVITE_RESPONSE_PROTOCOL,
                        contextInvitation.getContactId(),
                        connectionInfo);

            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public void rejectContextInvitation(ContextInvitation contextInvitation)
            throws DbException {
        contextManager.removeContextInvitation(contextInvitation.getContactId(),contextInvitation.getContextId());
        sendMessage(CONTEXT_INVITE_RESPONSE_PROTOCOL,
                    contextInvitation.getContactId(),
                    new ConnectionInfo().setConversationInfo(null,
                                                             contextInvitation.getContextId())
        );

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
