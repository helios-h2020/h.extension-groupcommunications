package eu.h2020.helios_social.modules.groupcommunications.api.utils;

public class MessageId extends Identifier {

    /**
     * Label for hashing messages to calculate their identifiers.
     */
    public static final String LABEL = "eu.h2020.helios_social.modules.groupcommunications/MESSAGE_ID";

    MessageId(String id) {
        super(id);
    }
}
