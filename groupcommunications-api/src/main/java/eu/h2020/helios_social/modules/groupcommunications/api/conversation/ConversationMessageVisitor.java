package eu.h2020.helios_social.modules.groupcommunications.api.conversation;

import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;

public interface ConversationMessageVisitor<T> {

	T visitMessageHeader(MessageHeader h);

}
