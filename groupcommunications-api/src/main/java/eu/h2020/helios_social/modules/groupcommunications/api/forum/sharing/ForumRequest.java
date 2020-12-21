package eu.h2020.helios_social.modules.groupcommunications.api.forum.sharing;

import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;

public class ForumRequest implements AbstractMessage {

    enum Type {
        INFO, INVITE
    }

    private Type requestType;
    private String forumId;

    public ForumRequest(Type requestType, String forumId) {
        this.requestType = requestType;
        this.forumId = forumId;
    }

    public Type getRequestType() {
        return requestType;
    }

    public String getForumId() {
        return forumId;
    }
}
