package eu.h2020.helios_social.modules.groupcommunications.api.utils;

import java.util.UUID;

public class GroupId extends Identifier {

    /**
     * Label for hashing groups to calculate their identifiers.
     */
    public static final String LABEL = "eu.h2020.helios_social.modules" +
            ".groupcommunications/GROUP_ID";
    private String groupId;


    public GroupId() {
        super(UUID.randomUUID().toString());
    }

    public GroupId(String groupId) {
        super(groupId);
    }

    @Override
    public String getId() {
        return super.getId();
    }
}
