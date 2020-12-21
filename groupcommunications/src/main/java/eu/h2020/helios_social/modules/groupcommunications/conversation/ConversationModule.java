package eu.h2020.helios_social.modules.groupcommunications.conversation;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.modules.groupcommunications.api.conversation.ConversationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.privateconversation.PrivateMessageFactory;
import eu.h2020.helios_social.modules.groupcommunications.privateconversation.PrivateMessageFactoryImpl;

@Module
public class ConversationModule {

	@Provides
	@Singleton
	ConversationManager providesConversationManager(
			ConversationManagerImpl privateConversationManager) {
		return privateConversationManager;
	}

	@Provides
	PrivateMessageFactory providesPrivateMessageFactory(
			PrivateMessageFactoryImpl privateMessageFactory) {
		return privateMessageFactory;
	}
}
