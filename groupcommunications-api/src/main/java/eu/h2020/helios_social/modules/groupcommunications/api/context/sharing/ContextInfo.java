package eu.h2020.helios_social.modules.groupcommunications.api.context.sharing;

import eu.h2020.helios_social.modules.groupcommunications.api.context.ContextType;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;

public class ContextInfo implements AbstractMessage {

	private String contextId;
	private String name;
	private ContextType contextType;
	private String jsonContext;
	private long timestamp;

	public ContextInfo(String contextId, String name, ContextType contextType,
			String jsonContext,
			long timestamp) {
		this.contextId = contextId;
		this.name = name;
		this.contextType = contextType;
		this.jsonContext = jsonContext;
		this.timestamp = timestamp;
	}

	public String getContextId() {
		return contextId;
	}

	public String getName() {
		return name;
	}

	public String jsonContext() {
		return jsonContext;
	}

	public ContextType getContextType() {
		return contextType;
	}

	public long getTimestamp() {
		return timestamp;
	}

}
