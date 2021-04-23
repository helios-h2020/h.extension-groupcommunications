package eu.h2020.helios_social.modules.groupcommunications;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Interaction;
import eu.h2020.helios_social.core.contextualegonetwork.Utils;
import eu.h2020.helios_social.core.contextualegonetwork.listeners.CreationListener;
import eu.h2020.helios_social.core.contextualegonetwork.listeners.LoggingListener;
import eu.h2020.helios_social.core.contextualegonetwork.listeners.RecoveryListener;
import eu.h2020.helios_social.core.contextualegonetwork.storage.LegacyStorage;
import eu.h2020.helios_social.modules.groupcommunications.attachment.AttachmentModule;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.ResourceDiscoveryModule;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.QueryReceiver;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.QueryResponseReceiver;
import eu.h2020.helios_social.modules.groupcommunications_utils.lifecycle.LifecycleManager;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.contact.ContactModule;
import eu.h2020.helios_social.modules.groupcommunications.contact.connection.ConnectionRequestReceiver;
import eu.h2020.helios_social.modules.groupcommunications.context.ContextModule;
import eu.h2020.helios_social.modules.groupcommunications.context.sharing.ContextInvitationReceiver;
import eu.h2020.helios_social.modules.groupcommunications.event.HeliosEventModule;
import eu.h2020.helios_social.modules.groupcommunications.forum.ForumModule;
import eu.h2020.helios_social.modules.groupcommunications.forum.mebership.MembershipReceiver;
import eu.h2020.helios_social.modules.groupcommunications.group.GroupModule;
import eu.h2020.helios_social.modules.groupcommunications.group.sharing.GroupInvitationReceiver;
import eu.h2020.helios_social.modules.groupcommunications.messaging.MessagingModule;
import eu.h2020.helios_social.modules.groupcommunications.conversation.ConversationModule;
import eu.h2020.helios_social.modules.groupcommunications.mining.MiningModule;
import eu.h2020.helios_social.modules.groupcommunications.privateconversation.PrivateMessageReceiver;
import eu.h2020.helios_social.modules.groupcommunications.privategroup.PrivateGroupModule;
import eu.h2020.helios_social.modules.groupcommunications.profile.ProfileModule;
import eu.h2020.helios_social.modules.groupcommunications.profile.sharing.ProfileRequestReceiver;
import eu.h2020.helios_social.modules.groupcommunications.utils.InternalStorageConfig;
import eu.h2020.helios_social.modules.socialgraphmining.GNN.GNNMiner;
import eu.h2020.helios_social.modules.socialgraphmining.GNN.operations.Optimizer;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.CONTEXT_INVITE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.CONTEXT_INVITE_RESPONSE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.FORUM_MEMBERSHIP_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.GROUP_INVITE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.GROUP_INVITE_RESPONSE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.LOCATION_QUERY_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.PRIVATE_MESSAGE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.QUERY_RESPONSE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.REQUEST_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.RESPONSE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.TEXT_QUERY_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionConstants.CONNECTIONS_RECEIVER_ID;

@Module(includes = {
        AttachmentModule.class,
        ContactModule.class,
        ProfileModule.class,
        PrivateGroupModule.class,
        ForumModule.class,
        GroupModule.class,
        MessagingModule.class,
        ConversationModule.class,
        ContextModule.class,
        MiningModule.class,
        ResourceDiscoveryModule.class,
        HeliosEventModule.class
})
public class GroupCommunicationsModule {

    public static class EagerSingletons {
        @Inject
        CommunicationManager communicationManager;
        @Inject
        ContextualEgoNetwork egoNetwork;
    }

    @Provides
    @Singleton
    CommunicationManager provideCommunicationManager(
            LifecycleManager lifecycleManager,
            ReliableCommunicationManagerImpl communicationManager,
            ConnectionRequestReceiver connectionRequestReceiver,
            PrivateMessageReceiver privateMessageReceiver,
            ContextInvitationReceiver contextInvitationReceiver,
            GroupInvitationReceiver groupInvitationReceiver,
            MembershipReceiver membershipReceiver,
            ProfileRequestReceiver requestReceiver,
            QueryReceiver queryReceiver,
            QueryResponseReceiver queryResponseReceiver) {
        communicationManager.registerReceiver(CONNECTIONS_RECEIVER_ID,
                connectionRequestReceiver);
        communicationManager.registerReceiver(PRIVATE_MESSAGE_PROTOCOL,
                privateMessageReceiver);
        communicationManager.registerReceiver(CONTEXT_INVITE_PROTOCOL,
                contextInvitationReceiver);
        communicationManager.registerReceiver(CONTEXT_INVITE_RESPONSE_PROTOCOL,
                contextInvitationReceiver);
        communicationManager
                .registerReceiver(GROUP_INVITE_PROTOCOL,
                        groupInvitationReceiver);
        communicationManager.registerReceiver(GROUP_INVITE_RESPONSE_PROTOCOL,
                groupInvitationReceiver);
        communicationManager.registerReceiver(FORUM_MEMBERSHIP_PROTOCOL,
                membershipReceiver);
        communicationManager.registerReceiver(REQUEST_PROTOCOL, requestReceiver);
        communicationManager.registerReceiver(RESPONSE_PROTOCOL, requestReceiver);
        communicationManager.registerReceiver(TEXT_QUERY_PROTOCOL, queryReceiver);
        communicationManager.registerReceiver(LOCATION_QUERY_PROTOCOL, queryReceiver);
        communicationManager.registerReceiver(QUERY_RESPONSE_PROTOCOL, queryResponseReceiver);
        lifecycleManager.registerOpenDatabaseHook(communicationManager);
        lifecycleManager.registerService(communicationManager);
        return communicationManager;
    }

    @Provides
    @Singleton
    ContextualEgoNetwork provideContextualEgoNetwork(
            InternalStorageConfig config) {
        Utils.development = true;
        ContextualEgoNetwork egoNetwork = ContextualEgoNetwork.createOrLoad(
                new LegacyStorage(config.getStorageDir().getPath().toString() +
                        File.separator), "ego", "null");
        GNNMiner.class.getDeclaredConstructors();
        egoNetwork.addListener(
                new RecoveryListener());//automatic saving with minimal overhead
        egoNetwork.addListener(new CreationListener());//keep timestamps
        egoNetwork.addListener(new LoggingListener());//print events
        //Some needed non-sense
        Interaction.class.getDeclaredConstructors();
        Optimizer.Adam.class.getDeclaredConstructors();
        Optimizer.Regularization.class.getDeclaredConstructors();
        Optimizer.class.getDeclaredConstructors();

        egoNetwork.setCurrent(egoNetwork.getOrCreateContext("All%All"));
        egoNetwork.save();
        return egoNetwork;
    }
}
