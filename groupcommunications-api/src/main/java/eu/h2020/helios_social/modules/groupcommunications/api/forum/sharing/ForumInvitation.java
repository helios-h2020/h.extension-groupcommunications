package eu.h2020.helios_social.modules.groupcommunications.api.forum.sharing;

import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;

public class ForumInvitation implements AbstractMessage {

    private Forum forum;
    private String msg;

    public ForumInvitation(Forum forum, String msg) {
        this.forum = forum;
        this.msg = msg;
    }

    public Forum getForum() {
        return forum;
    }

    public String getMsg() {
        return msg;
    }
}
