package eu.h2020.helios_social.modules.groupcommunications.api.sharing;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;

public class Invitation {

	private ContactId contactId;
	private String contextId;
	private String name;
	private String json;
	private long timestamp;
	private boolean incoming;

	public Invitation(ContactId contactId, String contextId, String name, String json,
			long timestamp, boolean incoming) {
		this.contactId = contactId;
		this.contextId = contextId;
		this.name = name;
		this.json = json;
		this.timestamp = timestamp;
		this.incoming = incoming;
	}

	public ContactId getContactId(){
		return contactId;
	}

	public String getContextId() {
		return contextId;
	}

	public String getName() {
		return name;
	}

	public String getJson() {
		return json;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public boolean isIncoming() {
		return incoming;
	}
}
