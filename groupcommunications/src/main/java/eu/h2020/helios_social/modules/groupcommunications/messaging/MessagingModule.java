package eu.h2020.helios_social.modules.groupcommunications.messaging;

import android.app.Application;
import android.content.SharedPreferences;

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTracker;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessagingManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;
import io.tus.android.client.TusPreferencesURLStore;
import io.tus.java.client.TusClient;

@Module
public class MessagingModule {

    public static class EagerSingletons {
        @Inject
        MessagingManager messagingManager;
    }

    @Provides
    @Singleton
    MessageTracker providesMessageTracker(MessageTrackerImpl messageTracker) {
        return messageTracker;
    }

    @Provides
    @Singleton
    MessagingManager providesMessagingManager(EventBus eventBus, MessagingManagerImpl messagingManager) {
        eventBus.addListener(messagingManager);
        return messagingManager;
    }

    @Provides
    @Singleton
    TusClient providesTusClient(Application app) {
        SharedPreferences pref = app.getSharedPreferences("tus", 0);
        TusClient client = new TusClient();
        String url = app.getString(eu.h2020.helios_social.modules.filetransfer.R.string.TUS_URL);
        try {
            client.setUploadCreationURL(new URL(url));
            client.enableResuming(new TusPreferencesURLStore(pref));
            return client;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
