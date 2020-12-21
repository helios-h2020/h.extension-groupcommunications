package eu.h2020.helios_social.modules.groupcommunications.api.group;

/**
 * A Group can be considered an independent synchronization scope
 */
public class Group {

	protected String groupId;
	protected String contextId;
	protected byte[] descriptor;
	protected GroupType groupType;

	/**
	 * Each group should characterized by a unique Identifier
	 *
	 * @param groupId
	 */
	public Group(String groupId, String contextId, GroupType groupType) {
		this.groupId = groupId;
		this.contextId = contextId;
		this.groupType = groupType;
	}

	public Group(String groupId, String contextId, byte[] descriptor,
			GroupType groupType) {
		this.groupId = groupId;
		this.contextId = contextId;
		this.descriptor = descriptor;
		this.groupType = groupType;
	}

	/**
	 * @return the unique identifier of the group
	 */
	public String getId() {
		return groupId;
	}

	public String getContextId() {
		return contextId;
	}

	public byte[] getDescriptor() {
		return descriptor;
	}

	public GroupType getGroupType() {
		return groupType;
	}

	@Override
	public int hashCode() {
		return groupId.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Group && groupId.equals(((Group) o).groupId);
	}

	@Override
	public String toString() {
		return "Group{" +
				"groupId='" + groupId + '\'' +
				", contextId='" + contextId + '\'' +
				'}';
	}
}
