package eu.h2020.helios_social.modules.groupcommunications.api;

public class CommunicationConstants {

	public static final String EGO_ID = "eu.h2020.helios_social.egoId";
	public static final String USERNAME = "eu.h2020.helios_social.username";
	public static final String APP_TAG = "helios-talk";
	public static final String PEERID = "eu.h2020.helios_social.peerId";

	public static final String PRIVATE_MESSAGE_PROTOCOL =
			"/helios-talk/private-messaging";
	public static final String CONTEXT_INVITE_PROTOCOL =
			"/helios-talk/context/invite";
	public static final String CONTEXT_INVITE_RESPONSE_PROTOCOL =
			"/helios-talk/context/response";

	public static final String GROUP_INVITE_PROTOCOL =
			"/helios-talk/group/invite";
	public static final String GROUP_INVITE_RESPONSE_PROTOCOL =
			"/helios-talk/group/response";

	public static final String FORUM_MEMBERSHIP_PROTOCOL =
			"/helios-talk/forum/membership/update-role";
}
