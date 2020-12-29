package eu.h2020.helios_social.modules.groupcommunications.profile.sharing;

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
import eu.h2020.helios_social.happ.helios.talk.api.event.EventBus;
import eu.h2020.helios_social.happ.helios.talk.api.sync.event.ProfileReceivedEvent;
import eu.h2020.helios_social.happ.helios.talk.api.sync.event.ProfileRequestReceivedEvent;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.Profile;
import eu.h2020.helios_social.modules.groupcommunications.api.sharing.Request;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.REQUEST_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.RESPONSE_PROTOCOL;

public class ProfileRequestReceiver implements HeliosMessagingReceiver {

    private final ContactManager contactManager;
    private final EventBus eventBus;

    @Inject
    public ProfileRequestReceiver(ContactManager contactManager, EventBus eventBus) {
        this.contactManager = contactManager;
        this.eventBus = eventBus;
    }

    @Override
    public void receiveMessage(@NotNull HeliosNetworkAddress heliosNetworkAddress,
                               @NotNull String protocolId, @NotNull FileDescriptor fileDescriptor) {
        if (!(protocolId.equals(REQUEST_PROTOCOL) ||
                protocolId.equals(RESPONSE_PROTOCOL))) return;
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
    public void receiveMessage(@NotNull HeliosNetworkAddress heliosNetworkAddress,
                               @NotNull String protocolId, @NotNull byte[] data) {
        String stringMessage = new String(data, StandardCharsets.UTF_8);
        ContactId contactId =
                new ContactId(heliosNetworkAddress.getNetworkId());
        if (protocolId.equals(REQUEST_PROTOCOL)) {
            Request request = new Gson().fromJson(stringMessage, Request.class);
            try {
                if (contactManager.contactExists(contactId)) {
                    eventBus.broadcast(
                            new ProfileRequestReceivedEvent(contactId, request.getContextId())
                    );
                }
            } catch (DbException e) {
                e.printStackTrace();
            }
        } else {
            Profile profile = new Gson().fromJson(stringMessage, Profile.class);
            eventBus.broadcast(new ProfileReceivedEvent(contactId, profile));
        }
    }
}
