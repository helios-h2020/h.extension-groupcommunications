package eu.h2020.helios_social.modules.groupcommunications.api.messaging;

public class GroupCount {

	private final int msgCount, unreadCount;
	private final long latestMsgTime;

	public GroupCount(int msgCount, int unreadCount, long latestMsgTime) {
		this.msgCount = msgCount;
		this.unreadCount = unreadCount;
		this.latestMsgTime = latestMsgTime;
	}

	public int getMsgCount() {
		return msgCount;
	}

	public int getUnreadCount() {
		return unreadCount;
	}

	public long getLatestMsgTime() {
		return latestMsgTime;
	}
}
