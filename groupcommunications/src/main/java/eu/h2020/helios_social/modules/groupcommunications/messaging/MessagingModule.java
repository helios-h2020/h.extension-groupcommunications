package eu.h2020.helios_social.modules.groupcommunications.messaging;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTracker;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessagingManager;

@Module
public class MessagingModule {

	@Provides
	@Singleton
	MessageTracker providesMessageTracker(MessageTrackerImpl messageTracker) {
		return messageTracker;
	}

	@Provides
	@Singleton
	MessagingManager providesMessagingManager(MessagingManagerImpl messageTracker) {
		return messageTracker;
	}
}
