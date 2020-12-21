package eu.h2020.helios_social.modules.groupcommunications.api.group;


import org.jetbrains.annotations.NotNull;

import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;

/**
 * A class for creating new non-sharable Groups of conversation (only owner can share the group)
 */
public interface GroupFactory {

	/**
	 * Creates a non-sharable Group of conversation with a given name
	 *
	 * @param name of the Private Group
	 */
	PrivateGroup createPrivateGroup(@NotNull String name, String contextId);

}
