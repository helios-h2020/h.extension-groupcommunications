package eu.h2020.helios_social.modules.groupcommunications.api.privategroup;

import eu.h2020.helios_social.modules.groupcommunications.api.messaging.GroupMessage;

public interface GroupMessageFactory {

	GroupMessage createGroupMessage(String groupId,
			String text, long timestamp, String funnyName, String fakeId);
}
