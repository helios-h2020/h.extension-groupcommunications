package eu.h2020.helios_social.modules.groupcommunications.api.peer;

public class PeerId {

	public String pid;
	public String fakeId;

	public PeerId(String pid) {
		this.pid = pid;
	}

	public PeerId(String pid, String fakeId) {
		this.pid = pid;
		this.fakeId = fakeId;
	}

	public String getId() {
		return pid;
	}

	public String getFakeId() {
		return fakeId;
	}
}
