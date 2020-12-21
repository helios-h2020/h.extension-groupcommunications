package eu.h2020.helios_social.modules.groupcommunications.api.group.sharing;

import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitation;

public interface SharingGroupManager {

    void sendGroupInvitation(GroupInvitation privateGroupInvite) throws DbException;

    void acceptGroupInvitation(GroupInvitation privateGroupInvite)
            throws DbException, FormatException;

    void rejectGroupInvitation(
            GroupInvitation privateGroupInvite) throws FormatException, DbException;
}
