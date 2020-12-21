package eu.h2020.helios_social.modules.groupcommunications.api.contact;

import javax.annotation.Nullable;

public class PendingContact extends Contact {

    private PendingContactType pendingContactType;
    private long timestamp;
    private String message;

    public PendingContact(ContactId id, @Nullable String alias,
                          PendingContactType pendingContactType, String message, long timestamp) {
        super(id, alias);
        this.pendingContactType = pendingContactType;
        this.message = message;
        this.timestamp = timestamp;
    }

    public PendingContactType getPendingContactType() {
        return pendingContactType;
    }

    public String getMessage(){
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
