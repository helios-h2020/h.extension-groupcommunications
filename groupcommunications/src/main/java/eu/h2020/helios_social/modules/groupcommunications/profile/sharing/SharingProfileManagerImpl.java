package eu.h2020.helios_social.modules.groupcommunications.profile.sharing;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.Event;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventListener;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.ProfileRequestReceivedEvent;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.Profile;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.ProfileManager;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.sharing.SharingProfileManager;
import eu.h2020.helios_social.modules.groupcommunications.api.sharing.Request;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.REQUEST_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.RESPONSE_PROTOCOL;
import static java.util.logging.Logger.getLogger;

public class SharingProfileManagerImpl implements SharingProfileManager, EventListener {
    private static final Logger LOG =
            getLogger(SharingProfileManagerImpl.class.getName());

    private final CommunicationManager communicationManager;
    private final ProfileManager profileManager;

    @Inject
    public SharingProfileManagerImpl(
            CommunicationManager communicationManager,
            ProfileManager profileManager) {
        this.communicationManager = communicationManager;
        this.profileManager = profileManager;
    }


    @Override
    public void sendProfileRequest(ContactId contactId, String contextId) {
        Request request = new Request(contextId, Request.Type.PROFILE);
        try {
            communicationManager.sendDirectMessage(REQUEST_PROTOCOL, contactId, request);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendProfileResponse(ContactId contactId, String contextId) throws DbException {
        Profile profile = profileManager.getProfile(contextId);
        try {
            communicationManager.sendDirectMessage(RESPONSE_PROTOCOL, contactId, profile);
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
        if (e instanceof ProfileRequestReceivedEvent) {
            try {
                LOG.info("profile request received and now sending response");
                sendProfileResponse(
                        ((ProfileRequestReceivedEvent) e).getContactId(),
                        ((ProfileRequestReceivedEvent) e).getContextId()
                );
            } catch (DbException ex) {
                ex.printStackTrace();
            }
        }
    }
}
